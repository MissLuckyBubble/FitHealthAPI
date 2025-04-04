package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.UserPreference;
import fit.health.fithealthapi.model.enums.UserItemType;
import fit.health.fithealthapi.services.UserPreferenceService;
import fit.health.fithealthapi.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;
    private final UserService userService;

    @PostMapping("/like")
    public ResponseEntity<UserPreference> like(
            @RequestParam UserItemType type,
            @RequestParam Long itemId
    ) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(preferenceService.like(user, type, itemId));
    }

    @PostMapping("/dislike")
    public ResponseEntity<UserPreference> dislike(
            @RequestParam UserItemType type,
            @RequestParam Long itemId
    ) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(preferenceService.dislike(user, type, itemId));
    }

    @GetMapping("/liked")
    public ResponseEntity<List<UserPreference>> getLiked( UserItemType type) {
        User user = getAuthenticatedUser();
        List<UserPreference> likes = preferenceService.getLikedItemsByType(user, type);
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/disliked")
    public ResponseEntity<List<UserPreference>> getDisliked(@RequestParam UserItemType type ) {
        User user = getAuthenticatedUser();
        List<UserPreference> dislikes = preferenceService.getDislikedItemsByType(user, type);
        return ResponseEntity.ok(dislikes);
    }

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByUsername(username);
    }
}