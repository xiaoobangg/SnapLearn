package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.*;
import com.snaplearn.mapper.*;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.AdminService;
import com.snaplearn.service.StatsService;
import com.snaplearn.service.WordBankService;
import com.snaplearn.service.UserSettingsService;
import com.snaplearn.service.WordContentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StatsService statsService;
    private final UserMapper userMapper;
    private final CardGroupMapper cardGroupMapper;
    private final CardMapper cardMapper;
    private final UserDailyPoolMapper userDailyPoolMapper;
    private final DailyCheckinLogMapper dailyCheckinLogMapper;
    private final WordMapper wordMapper;
    private final WordContentMapper wordContentMapper;
    private final WordBankMapper wordBankMapper;
    private final WordContentService wordContentService;
    private final UserRoleMapper userRoleMapper;
    private final UserSettingsService userSettingsService;

    // ===== 认证 =====

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq req) {
        Map<String, Object> originalResult = adminService.login(req.username(), req.password());
        Map<String, Object> result = new HashMap<>(originalResult);
        result.put("is_default_pwd", AdminService.DEFAULT_ADMIN_PASSWORD.equals(req.password()));
        return result;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {
    }

    @PutMapping("/password")
    public Map<String, Object> changePassword(
            @RequestBody Map<String, String> body,
            @CurrentUser String userId
    ) {
        String oldPassword = body.get("old_password");
        String newPassword = body.get("new_password");
        if (newPassword == null || newPassword.length() < 6) {
            return Map.of("ok", false, "detail", "新密码至少6位");
        }
        adminService.changePassword(userId, oldPassword, newPassword);
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@CurrentUser String userId) {
        User user = adminService.getById(userId);
        var rq = new QueryWrapper<UserRole>().eq("user_id", userId);
        String role = userRoleMapper.selectList(rq).stream().map(UserRole::getRoleCode).findFirst().orElse("user");
        return Map.of(
                "id", user.getId(),
                "username", user.getPhone(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", role
        );
    }

    // ===== 仪表盘 =====

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return statsService.getOverview();
    }

    @GetMapping("/stats/overview")
    public Map<String, Object> overview() {
        return statsService.getOverview();
    }

    @GetMapping("/stats/daily")
    public List<Map<String, Object>> dailyStats(@RequestParam(defaultValue = "7") int days) {
        return statsService.getDailyStats(Math.min(days, 30));
    }

    // ===== 用户管理 =====

    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("phone", keyword).or().like("nickname", keyword).or().like("wechat_openid", keyword));
        }
        qw.orderByDesc("created_at");

        Page<User> pg = new Page<>(page, pageSize);
        Page<User> result = userMapper.selectPage(pg, qw);

        List<Map<String, Object>> items = result.getRecords().stream().map(u -> {
            long cardCount = cardMapper.selectCount(
                    new QueryWrapper<Card>().eq("user_id", u.getId()));
            long poolCount = userDailyPoolMapper.selectCount(
                    new QueryWrapper<UserDailyPool>().eq("user_id", u.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("phone", u.getPhone() != null ? u.getPhone() : "");
            m.put("nickname", u.getNickname() != null ? u.getNickname() : "");
            m.put("wechat_openid", u.getWechatOpenid() != null ? u.getWechatOpenid() : "");
            m.put("avatar_url", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
            m.put("card_count", cardCount);
            m.put("pool_count", poolCount);
            m.put("created_at", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", items);
        resp.put("total", result.getTotal());
        resp.put("page", page);
        resp.put("page_size", pageSize);
        return resp;
    }

    @PutMapping("/users/{id}/password")
    public Map<String, Object> resetUserPassword(@PathVariable String id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("new_password");
        if (newPassword == null || newPassword.length() < 6) {
            return Map.of("ok", false, "detail", "新密码至少6位");
        }
        adminService.resetUserPassword(id, newPassword);
        return Map.of("ok", true);
    }

    /** 获取用户 AI 对话偏好 */
    @GetMapping("/users/{id}/settings")
    public Map<String, Object> getUserSettings(@PathVariable String id) {
        return userSettingsService.getChatPreferences(id);
    }

    /** 更新用户 AI 对话偏好 */
    @PutMapping("/users/{id}/settings")
    public Map<String, Object> updateUserSettings(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String chatMode = body.get("chat_mode") != null ? body.get("chat_mode").toString() : null;
        String chatModel = body.get("chat_model") != null ? body.get("chat_model").toString() : null;
        Boolean chatStream = body.get("chat_stream") != null ? Boolean.valueOf(body.get("chat_stream").toString()) : null;
        userSettingsService.updateChatPreferences(id, chatMode, chatModel, chatStream);
        return Map.of("ok", true);
    }

    @GetMapping("/users/{id}/roles")
    public List<String> getUserRoles(@PathVariable String id) {
        return userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", id))
                .stream().map(UserRole::getRoleCode).toList();
    }

    @PutMapping("/users/{id}/roles")
    public Map<String, Object> setUserRoles(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        List<String> roles = body.getOrDefault("roles", List.of());
        // 删除旧角色
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
        // 分配新角色
        for (String roleCode : roles) {
            UserRole ur = new UserRole();
            ur.setId(UUID.randomUUID().toString());
            ur.setUserId(id);
            ur.setRoleCode(roleCode);
            userRoleMapper.insert(ur);
        }
        return Map.of("ok", true);
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> userDetail(@PathVariable String id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");

        long cardCount = cardMapper.selectCount(new QueryWrapper<Card>().eq("user_id", id));
        long groupCount = cardGroupMapper.selectCount(new QueryWrapper<CardGroup>().eq("user_id", id));
        long poolCount = userDailyPoolMapper.selectCount(
                new QueryWrapper<UserDailyPool>().eq("user_id", id));
        long masteredCount = userDailyPoolMapper.selectCount(
                new QueryWrapper<UserDailyPool>().eq("user_id", id).eq("pool_status", "mastered"));

        List<CardGroup> groups = cardGroupMapper.selectList(
                new QueryWrapper<CardGroup>().eq("user_id", id).orderByDesc("created_at"));

        List<Map<String, Object>> groupList = groups.stream().map(g -> {
            long gc = cardMapper.selectCount(new QueryWrapper<Card>().eq("group_id", g.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle() != null ? g.getTitle() : "");
            m.put("group_status", g.getGroupStatus() != null ? g.getGroupStatus() : "");
            m.put("source_text", g.getSourceText() != null ? g.getSourceText() : "");
            m.put("card_count", gc);
            m.put("created_at", g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
            return m;
        }).toList();

        // Checkin stats
        long checkinDays = dailyCheckinLogMapper.selectCount(
                new QueryWrapper<DailyCheckinLog>().eq("user_id", id));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", user.getId());
        resp.put("phone", user.getPhone() != null ? user.getPhone() : "");
        resp.put("nickname", user.getNickname() != null ? user.getNickname() : "");
        resp.put("avatar_url", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        resp.put("card_count", cardCount);
        resp.put("group_count", groupCount);
        resp.put("pool_count", poolCount);
        resp.put("mastered_count", masteredCount);
        resp.put("checkin_days", checkinDays);
        resp.put("groups", groupList);
        resp.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return resp;
    }

    // ===== 卡片组管理 =====

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return Map.of("ok", true);
    }

    @GetMapping("/groups")
    public Map<String, Object> listGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        QueryWrapper<CardGroup> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("title", keyword);
        }
        qw.orderByDesc("created_at");

        Page<CardGroup> pg = new Page<>(page, pageSize);
        Page<CardGroup> result = cardGroupMapper.selectPage(pg, qw);

        List<Map<String, Object>> items = result.getRecords().stream().map(g -> {
            User u = userMapper.selectById(g.getUserId());
            long cardCount = cardMapper.selectCount(
                    new QueryWrapper<Card>().eq("group_id", g.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle() != null ? g.getTitle() : "");
            m.put("group_status", g.getGroupStatus() != null ? g.getGroupStatus() : "");
            m.put("user_phone", u != null ? u.getPhone() : "");
            m.put("user_nickname", u != null ? u.getNickname() : "");
            m.put("source_text", g.getSourceText() != null ? g.getSourceText() : "");
            m.put("card_count", cardCount);
            m.put("created_at", g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", items);
        resp.put("total", result.getTotal());
        resp.put("page", page);
        resp.put("page_size", pageSize);
        return resp;
    }

    @GetMapping("/groups/{id}")
    public Map<String, Object> groupDetail(@PathVariable String id) {
        CardGroup group = cardGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(404, "卡片组不存在");

        List<Card> cards = cardMapper.selectList(
                new QueryWrapper<Card>().eq("group_id", id));
        User user = userMapper.selectById(group.getUserId());

        // Batch fetch word info
        Set<String> wordIds = new LinkedHashSet<>();
        for (Card c : cards) wordIds.add(c.getWordId());
        Map<String, Word> wordMap = new LinkedHashMap<>();
        Map<String, WordContent> contentMap = new LinkedHashMap<>();
        if (!wordIds.isEmpty()) {
            wordMapper.selectBatchIds(wordIds).forEach(w -> wordMap.put(w.getId(), w));
            wordContentMapper.selectList(
                    new QueryWrapper<WordContent>().in("word_id", wordIds)
            ).forEach(wc -> contentMap.put(wc.getWordId(), wc));
        }

        List<Map<String, Object>> cardList = cards.stream().map(c -> {
            Word word = wordMap.get(c.getWordId());
            WordContent wc = contentMap.get(c.getWordId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("word", word != null ? word.getWordText() : "");
            m.put("general_meaning", wc != null && wc.getGeneralMeaning() != null ? wc.getGeneralMeaning() : "");
            m.put("pos", wc != null && wc.getPos() != null ? wc.getPos() : "");
            m.put("card_status", c.getCardStatus() != null ? c.getCardStatus() : "");
            m.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", group.getId());
        resp.put("title", group.getTitle() != null ? group.getTitle() : "");
        resp.put("group_status", group.getGroupStatus() != null ? group.getGroupStatus() : "");
        resp.put("user_phone", user != null ? user.getPhone() : "");
        resp.put("user_nickname", user != null ? user.getNickname() : "");
        resp.put("source_text", group.getSourceText() != null ? group.getSourceText() : "");
        resp.put("source_image", group.getSourceImage() != null ? group.getSourceImage() : "");
        resp.put("cards", cardList);
        resp.put("created_at", group.getCreatedAt() != null ? group.getCreatedAt().toString() : "");
        return resp;
    }

    @DeleteMapping("/groups/{id}")
    public Map<String, Object> deleteGroup(@PathVariable String id) {
        CardGroup group = cardGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(404, "卡片组不存在");
        cardMapper.delete(new QueryWrapper<Card>().eq("group_id", id));
        cardGroupMapper.deleteById(id);
        return Map.of("ok", true);
    }

    // ===== 卡片管理 =====

    @GetMapping("/cards")
    public Map<String, Object> listCards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        // Search by word text via word table
        Set<String> matchingWordIds = new HashSet<>();
        if (keyword != null && !keyword.isEmpty()) {
            wordMapper.selectList(
                    new QueryWrapper<Word>().like("word_text", keyword)
            ).forEach(w -> matchingWordIds.add(w.getId()));
            if (matchingWordIds.isEmpty()) {
                return Map.of("items", List.of(), "total", 0, "page", page, "page_size", pageSize);
            }
        }

        QueryWrapper<Card> qw = new QueryWrapper<>();
        if (!matchingWordIds.isEmpty()) {
            qw.in("word_id", matchingWordIds);
        }
        qw.orderByDesc("created_at");

        Page<Card> pg = new Page<>(page, pageSize);
        Page<Card> result = cardMapper.selectPage(pg, qw);

        // Batch fetch words
        Set<String> wordIds = new LinkedHashSet<>();
        for (Card c : result.getRecords()) wordIds.add(c.getWordId());
        Map<String, Word> wordMap = new LinkedHashMap<>();
        Map<String, WordContent> contentMap = new LinkedHashMap<>();
        if (!wordIds.isEmpty()) {
            wordMapper.selectBatchIds(wordIds).forEach(w -> wordMap.put(w.getId(), w));
            wordContentMapper.selectList(
                    new QueryWrapper<WordContent>().in("word_id", wordIds)
            ).forEach(wc -> contentMap.put(wc.getWordId(), wc));
        }
        Map<String, User> userMap = new LinkedHashMap<>();
        Set<String> userIds = new HashSet<>();
        for (Card c : result.getRecords()) userIds.add(c.getUserId());
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        List<Map<String, Object>> items = result.getRecords().stream().map(c -> {
            Word word = wordMap.get(c.getWordId());
            WordContent wc = contentMap.get(c.getWordId());
            User u = userMap.get(c.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("word", word != null ? word.getWordText() : "");
            m.put("general_meaning", wc != null && wc.getGeneralMeaning() != null ? wc.getGeneralMeaning() : "");
            m.put("pos", wc != null && wc.getPos() != null ? wc.getPos() : "");
            m.put("card_status", c.getCardStatus() != null ? c.getCardStatus() : "");
            m.put("user_phone", u != null ? u.getPhone() : "");
            m.put("user_nickname", u != null ? u.getNickname() : "");
            m.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", items);
        resp.put("total", result.getTotal());
        resp.put("page", page);
        resp.put("page_size", pageSize);
        return resp;
    }

    // ===== 词库管理（新增） =====

    @GetMapping("/word-banks")
    public Map<String, Object> listWordBanks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        QueryWrapper<WordBank> qw = new QueryWrapper<>();
        qw.orderByDesc("created_at");
        Page<WordBank> pg = new Page<>(page, pageSize);
        Page<WordBank> result = wordBankMapper.selectPage(pg, qw);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", result.getRecords());
        resp.put("total", result.getTotal());
        resp.put("page", page);
        resp.put("page_size", pageSize);
        return resp;
    }

    @PostMapping("/word-banks")
    public Map<String, Object> createWordBank(@RequestBody CreateBankReq req, @CurrentUser String userId) {
        WordBankService wbs = new WordBankService(wordBankMapper,
                null, wordMapper, null); // admin creating preset bank
        // Note: simplified — actual implementation needs proper DI
        return Map.of("ok", true);
    }

    public record CreateBankReq(String name, String description) {
    }

    // ===== 单词内容管理（新增） =====

    @GetMapping("/word-contents")
    public Map<String, Object> listWordContents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        QueryWrapper<Word> wordQw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wordQw.like("word_text", keyword);
        }
        wordQw.orderByDesc("created_at");
        Page<Word> wordPg = new Page<>(page, pageSize);
        Page<Word> wordResult = wordMapper.selectPage(wordPg, wordQw);

        Set<String> wordIds = new LinkedHashSet<>();
        for (Word w : wordResult.getRecords()) wordIds.add(w.getId());
        Map<String, WordContent> contentMap = new LinkedHashMap<>();
        if (!wordIds.isEmpty()) {
            wordContentMapper.selectList(
                    new QueryWrapper<WordContent>().in("word_id", wordIds)
            ).forEach(wc -> contentMap.put(wc.getWordId(), wc));
        }

        List<Map<String, Object>> items = wordResult.getRecords().stream().map(w -> {
            WordContent wc = contentMap.get(w.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("word_id", w.getId());
            m.put("word_text", w.getWordText());
            m.put("pronunciation", wc != null ? wc.getPronunciation() : "");
            m.put("pos", wc != null ? wc.getPos() : "");
            m.put("general_meaning", wc != null ? wc.getGeneralMeaning() : "");
            m.put("extended_meaning", wc != null ? wc.getExtendedMeaning() : "");
            m.put("example_sentence", wc != null ? wc.getExampleSentence() : "");
            m.put("memory_tip", wc != null ? wc.getMemoryTip() : "");
            m.put("llm_version", wc != null ? wc.getLlmVersion() : "");
            m.put("updated_at", wc != null && wc.getUpdatedAt() != null ? wc.getUpdatedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", items);
        resp.put("total", wordResult.getTotal());
        resp.put("page", page);
        resp.put("page_size", pageSize);
        return resp;
    }

    @PostMapping("/word-contents/{wordId}/refresh")
    public Map<String, Object> refreshWordContent(@PathVariable String wordId) {
        Word word = wordMapper.selectById(wordId);
        if (word == null) throw new BusinessException(404, "单词不存在");
        WordContent wc = wordContentService.refreshContent(wordId, word.getWordText());
        return Map.of("word_content", wc);
    }
}
