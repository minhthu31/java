package vn.edu.cnpm.projectsupport.organization;

import java.sql.Date;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.RoleRepository;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.CreatePersonRequest;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.GroupRequest;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.GroupResponse;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.UserSummary;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;

@Service
@Transactional
public class OrganizationService {
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(JdbcTemplate jdbc, CurrentUserService currentUserService,
            UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> groupsForAdmin() {
        requireRole(RoleCode.ADMIN);
        return jdbc.query("SELECT g.id,g.code,g.name,g.leader_user_id,u.full_name,g.start_date,g.end_date,g.status,"
                        + "(SELECT COUNT(*) FROM group_members gm WHERE gm.group_id=g.id AND gm.status='ACTIVE') member_count "
                        + "FROM student_groups g LEFT JOIN users u ON u.id=g.leader_user_id ORDER BY g.code",
                (rs, n) -> new GroupResponse(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        nullableLong(rs, "leader_user_id"), rs.getString("full_name"),
                        rs.getDate("start_date") == null ? null : rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate(),
                        rs.getString("status"), rs.getLong("member_count"), lecturers(rs.getLong("id"))));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> groupsForLecturer() {
        User user = requireRole(RoleCode.LECTURER);
        return jdbc.query("SELECT g.id,g.code,g.name,g.leader_user_id,u.full_name,g.start_date,g.end_date,g.status,"
                        + "(SELECT COUNT(*) FROM group_members gm WHERE gm.group_id=g.id AND gm.status='ACTIVE') member_count "
                        + "FROM student_groups g JOIN group_lecturers gl ON gl.group_id=g.id "
                        + "LEFT JOIN users u ON u.id=g.leader_user_id WHERE gl.lecturer_user_id=? ORDER BY g.code",
                (rs, n) -> new GroupResponse(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        nullableLong(rs, "leader_user_id"), rs.getString("full_name"),
                        rs.getDate("start_date") == null ? null : rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate(),
                        rs.getString("status"), rs.getLong("member_count"), lecturers(rs.getLong("id"))), user.getId());
    }

    public GroupResponse createGroup(GroupRequest request) {
        requireRole(RoleCode.ADMIN);
        validateLeader(request.leaderUserId());
        try {
            jdbc.update("INSERT INTO student_groups(code,name,leader_user_id,start_date,end_date,status) VALUES(?,?,?,?,?,?)",
                    request.code().trim(), request.name().trim(), request.leaderUserId(), date(request.startDate()),
                    date(request.endDate()), normalizeStatus(request.status()));
        } catch (DuplicateKeyException ex) {
            throw new ResourceInUseException("Mã nhóm đã tồn tại");
        }
        Long id = jdbc.queryForObject("SELECT id FROM student_groups WHERE code=?", Long.class, request.code().trim());
        return group(id);
    }

    public GroupResponse updateGroup(Long groupId, GroupRequest request) {
        requireRole(RoleCode.ADMIN);
        requireGroup(groupId);
        validateLeader(request.leaderUserId());
        jdbc.update("UPDATE student_groups SET code=?,name=?,leader_user_id=?,start_date=?,end_date=?,status=? WHERE id=?",
                request.code().trim(), request.name().trim(), request.leaderUserId(), date(request.startDate()),
                date(request.endDate()), normalizeStatus(request.status()), groupId);
        return group(groupId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> lecturers() {
        requireRole(RoleCode.ADMIN);
        return usersByRole(RoleCode.LECTURER);
    }

    public UserSummary createLecturer(CreatePersonRequest request) {
        requireRole(RoleCode.ADMIN);
        return createUser(request, RoleCode.LECTURER);
    }

    public void assignLecturer(Long groupId, Long lecturerId) {
        requireRole(RoleCode.ADMIN);
        requireGroup(groupId);
        requireUserRole(lecturerId, RoleCode.LECTURER);
        jdbc.update("INSERT INTO group_lecturers(group_id,lecturer_user_id) VALUES(?,?) ON DUPLICATE KEY UPDATE assigned_at=assigned_at",
                groupId, lecturerId);
    }

    public void unassignLecturer(Long groupId, Long lecturerId) {
        requireRole(RoleCode.ADMIN);
        requireGroup(groupId);
        jdbc.update("DELETE FROM group_lecturers WHERE group_id=? AND lecturer_user_id=?", groupId, lecturerId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> members(Long groupId) {
        requireGroupAccess(groupId);
        return jdbc.query("SELECT u.id,u.username,u.email,u.full_name,r.code,u.status FROM users u "
                        + "JOIN roles r ON r.id=u.role_id JOIN group_members gm ON gm.user_id=u.id "
                        + "WHERE gm.group_id=? AND gm.status='ACTIVE' ORDER BY u.full_name",
                (rs, n) -> userSummary(rs), groupId);
    }

    public UserSummary createMember(Long groupId, CreatePersonRequest request) {
        requireGroupAccess(groupId);
        UserSummary user = createUser(request, RoleCode.TEAM_MEMBER);
        assignMember(groupId, user.id());
        return user;
    }

    public void assignMember(Long groupId, Long userId) {
        requireGroupAccess(groupId);
        requireUserRole(userId, RoleCode.TEAM_MEMBER);
        jdbc.update("INSERT INTO group_members(group_id,user_id,member_role,status) VALUES(?,?,'TEAM_MEMBER','ACTIVE') "
                        + "ON DUPLICATE KEY UPDATE status='ACTIVE',left_at=NULL", groupId, userId);
    }

    public void removeMember(Long groupId, Long userId) {
        requireGroupAccess(groupId);
        jdbc.update("UPDATE group_members SET status='INACTIVE',left_at=CURRENT_TIMESTAMP(6) WHERE group_id=? AND user_id=?",
                groupId, userId);
        jdbc.update("UPDATE tasks t JOIN projects p ON p.id=t.project_id SET t.assignee_user_id=NULL "
                + "WHERE p.group_id=? AND t.assignee_user_id=? AND t.status NOT IN ('DONE','CANCELLED')", groupId, userId);
    }

    private UserSummary createUser(CreatePersonRequest request, RoleCode roleCode) {
        if (userRepository.existsByUsernameIgnoreCase(request.username().trim())
                || userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ResourceInUseException("Username hoặc email đã tồn tại");
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò không tồn tại"));
        User saved = userRepository.save(new User(role, request.username().trim(), request.email().trim(),
                passwordEncoder.encode(request.initialPassword()), request.fullName().trim()));
        return new UserSummary(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getFullName(),
                roleCode.name(), saved.getStatus().name());
    }

    private List<UserSummary> usersByRole(RoleCode role) {
        return jdbc.query("SELECT u.id,u.username,u.email,u.full_name,r.code,u.status FROM users u JOIN roles r ON r.id=u.role_id WHERE r.code=? ORDER BY u.full_name",
                (rs, n) -> userSummary(rs), role.name());
    }

    private List<UserSummary> lecturers(Long groupId) {
        return jdbc.query("SELECT u.id,u.username,u.email,u.full_name,r.code,u.status FROM users u JOIN roles r ON r.id=u.role_id JOIN group_lecturers gl ON gl.lecturer_user_id=u.id WHERE gl.group_id=? ORDER BY u.full_name",
                (rs, n) -> userSummary(rs), groupId);
    }

    private GroupResponse group(Long groupId) {
        return groupsForAdmin().stream().filter(g -> g.id().equals(groupId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại"));
    }

    private void requireGroupAccess(Long groupId) {
        User user = currentUserService.findCurrentUser().orElseThrow(() -> new AccessDeniedException("Chưa xác thực"));
        requireGroup(groupId);
        if (user.getRole().getCode() == RoleCode.ADMIN) return;
        if (user.getRole().getCode() != RoleCode.LECTURER
                || jdbc.queryForObject("SELECT COUNT(*) FROM group_lecturers WHERE group_id=? AND lecturer_user_id=?",
                        Long.class, groupId, user.getId()) == 0) {
            throw new AccessDeniedException("Không được quản lý nhóm này");
        }
    }

    private User requireRole(RoleCode role) {
        User user = currentUserService.findCurrentUser().orElseThrow(() -> new AccessDeniedException("Chưa xác thực"));
        if (user.getRole().getCode() != role) throw new AccessDeniedException("Không đúng vai trò");
        return user;
    }

    private void requireGroup(Long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM student_groups WHERE id=?", Long.class, id) == 0)
            throw new ResourceNotFoundException("Nhóm không tồn tại");
    }

    private void requireUserRole(Long id, RoleCode role) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=? AND r.code=? AND u.status='ACTIVE'",
                Long.class, id, role.name());
        if (count == 0) throw new ResourceNotFoundException("Người dùng không tồn tại hoặc sai vai trò");
    }

    private void validateLeader(Long id) { if (id != null) requireUserRole(id, RoleCode.TEAM_LEADER); }
    private static Date date(java.time.LocalDate value) { return value == null ? null : Date.valueOf(value); }
    private static String normalizeStatus(String value) { return value == null || value.isBlank() ? "ACTIVE" : value.toUpperCase(); }
    private static Long nullableLong(java.sql.ResultSet rs, String name) throws java.sql.SQLException { long v=rs.getLong(name); return rs.wasNull()?null:v; }
    private static UserSummary userSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new UserSummary(rs.getLong("id"), rs.getString("username"), rs.getString("email"),
                rs.getString("full_name"), rs.getString("code"), rs.getString("status"));
    }
}
