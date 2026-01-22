package ru.nikogosyan.CourseProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nikogosyan.CourseProject.entity.Actor;
import ru.nikogosyan.CourseProject.entity.ActorPhoto;
import ru.nikogosyan.CourseProject.repository.ActorPhotoRepository;
import ru.nikogosyan.CourseProject.utils.SecurityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorPhotoService {

    private final ActorPhotoRepository actorPhotoRepository;
    private final ActorService actorService;
    private final SecurityUtils securityUtils;

    private static final Path UPLOADDIR = Paths.get("uploads/actors");

    @Transactional(readOnly = true)
    public List<ActorPhoto> getPhotos(Long actorId) {
        return actorPhotoRepository.findByActorIdOrderByIsPrimaryDescCreatedAtAscIdAsc(actorId);
    }

    @Transactional(readOnly = true)
    public String getPrimaryOrFirstPhotoPath(Long actorId) {
        return actorPhotoRepository.findFirstByActorIdOrderByIsPrimaryDescCreatedAtAscIdAsc(actorId)
                .map(ActorPhoto::getImagePath)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getPrimaryOrFirstPhotoPaths(List<Long> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) return Map.of();

        List<ActorPhoto> photos = actorPhotoRepository.findForActorsOrdered(actorIds);

        Map<Long, String> result = new HashMap<>();
        for (ActorPhoto p : photos) {
            Long actorId = p.getActor().getId();
            result.putIfAbsent(actorId, p.getImagePath());
        }
        return result;
    }

    @Transactional
    public void upload(Long actorId, MultipartFile imageFile, Authentication authentication) throws IOException {
        Actor actor = actorService.getActorById(actorId);
        checkCanModify(authentication, actor);

        validateImage(imageFile);
        String webPath = storeFile(imageFile);

        boolean makePrimary = !actorPhotoRepository.existsByActorIdAndIsPrimaryTrue(actorId);
        savePhoto(actor, webPath, makePrimary);
    }

    @Transactional
    public void uploadPrimary(Long actorId, MultipartFile imageFile, Authentication authentication) throws IOException {
        Actor actor = actorService.getActorById(actorId);
        checkCanModify(authentication, actor);

        validateImage(imageFile);
        String webPath = storeFile(imageFile);

        actorPhotoRepository.clearPrimary(actorId);
        savePhoto(actor, webPath, true);
    }

    private void validateImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new RuntimeException("Файл пуст");
        }
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image")) {
            throw new RuntimeException("Разрешены только файлы изображений");
        }
    }

    private String storeFile(MultipartFile imageFile) throws IOException {
        if (!Files.exists(UPLOADDIR)) Files.createDirectories(UPLOADDIR);

        String ext = getExtension(imageFile.getOriginalFilename());
        String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;

        Path filePath = UPLOADDIR.resolve(fileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/actors/" + fileName;
    }

    private void savePhoto(Actor actor, String webPath, boolean primary) {
        ActorPhoto photo = new ActorPhoto();
        photo.setActor(actor);
        photo.setImagePath(webPath);
        photo.setPrimary(primary);
        actorPhotoRepository.save(photo);
    }


    @Transactional
    public void setPrimary(Long actorId, Long photoId, Authentication authentication) {
        Actor actor = actorService.getActorById(actorId);
        checkCanModify(authentication, actor);

        ActorPhoto photo = actorPhotoRepository.findByIdAndActorId(photoId, actorId)
                .orElseThrow(() -> new RuntimeException("Фотография не найдена"));

        actorPhotoRepository.clearPrimary(actorId);
        photo.setPrimary(true);
        actorPhotoRepository.save(photo);
    }

    @Transactional
    public void delete(Long actorId, Long photoId, Authentication authentication) throws IOException {
        Actor actor = actorService.getActorById(actorId);
        checkCanModify(authentication, actor);

        ActorPhoto photo = actorPhotoRepository.findByIdAndActorId(photoId, actorId)
                .orElseThrow(() -> new RuntimeException("Фотография не найдена"));

        String path = photo.getImagePath();
        if (path != null && path.startsWith("/uploads/")) {
            Path fsPath = Paths.get("uploads").resolve(path.substring("/uploads/".length()));
            Files.deleteIfExists(fsPath);
        }

        actorPhotoRepository.delete(photo);

        actorPhotoRepository.findFirstByActorIdOrderByIsPrimaryDescCreatedAtAscIdAsc(actorId)
                .ifPresent(p -> {
                    if (!p.isPrimary()) {
                        actorPhotoRepository.clearPrimary(actorId);
                        p.setPrimary(true);
                        actorPhotoRepository.save(p);
                    }
                });
    }

    private void checkCanModify(Authentication authentication, Actor actor) {
        if (securityUtils.isReadOnly(authentication)) {
            throw new RuntimeException("Пользователи, доступные только для чтения, не могут изменять данные");
        }
        boolean isAdmin = securityUtils.isAdmin(authentication);
        String username = authentication.getName();
        String createdBy = actor.getCreatedBy();

        if (!isAdmin && createdBy != null && !createdBy.equals(username)) {
            throw new RuntimeException("У вас нет прав на изменение этого субъекта");
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank() || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
