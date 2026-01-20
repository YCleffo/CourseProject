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
import java.nio.file.*;
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

    private static final Path UPLOAD_DIR = Paths.get("uploads/actors");

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
        if (actorIds == null || actorIds.isEmpty()) {
            return Map.of();
        }

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

        if (imageFile == null || imageFile.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image")) {
            throw new RuntimeException("Only image files are allowed");
        }

        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        String ext = getExtension(imageFile.getOriginalFilename());
        String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;
        Path filePath = UPLOAD_DIR.resolve(fileName);

        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        boolean makePrimary = !actorPhotoRepository.existsByActorIdAndIsPrimaryTrue(actorId);

        ActorPhoto photo = new ActorPhoto();
        photo.setActor(actor);
        photo.setImagePath("/uploads/actors/" + fileName);
        photo.setPrimary(makePrimary);

        actorPhotoRepository.save(photo);
    }

    @Transactional
    public void setPrimary(Long actorId, Long photoId, Authentication authentication) {
        Actor actor = actorService.getActorById(actorId);
        checkCanModify(authentication, actor);

        ActorPhoto photo = actorPhotoRepository.findByIdAndActorId(photoId, actorId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        actorPhotoRepository.clearPrimary(actorId);
        photo.setPrimary(true);
        actorPhotoRepository.save(photo);
    }

    @Transactional
    public void delete(Long actorId, Long photoId, Authentication authentication) throws IOException {
        Actor actor = actorService.getActorById(actorId);
        checkCanModify(authentication, actor);

        ActorPhoto photo = actorPhotoRepository.findByIdAndActorId(photoId, actorId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        String path = photo.getImagePath();
        if (path != null && path.startsWith("/uploads/")) {
            Path fsPath = Paths.get("uploads").resolve(path.substring("/uploads/".length()));
            Files.deleteIfExists(fsPath);
        }

        actorPhotoRepository.delete(photo);

        actorPhotoRepository.findFirstByActorIdOrderByIsPrimaryDescCreatedAtAscIdAsc(actorId).ifPresent(p -> {
            if (!p.isPrimary()) {
                actorPhotoRepository.clearPrimary(actorId);
                p.setPrimary(true);
                actorPhotoRepository.save(p);
            }
        });
    }

    private void checkCanModify(Authentication authentication, Actor actor) {
        if (securityUtils.isReadOnly(authentication)) {
            throw new RuntimeException("READ_ONLY users cannot modify data");
        }

        boolean isAdmin = securityUtils.isAdmin(authentication);
        String username = authentication.getName();
        String createdBy = actor.getCreatedBy();

        if (!isAdmin && createdBy != null && !createdBy.equals(username)) {
            throw new RuntimeException("You don't have permission to modify this actor");
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank() || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
