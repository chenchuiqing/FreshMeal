package com.freshmeal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.freshmeal.entity.Admin;
import com.freshmeal.mapper.AdminMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StpInterfaceImplTest {

    @Mock
    private AdminMapper adminMapper;

    @InjectMocks
    private StpInterfaceImpl stpInterface;

    @Test
    void userAndRiderRolesComeFromLoginType() {
        assertThat(stpInterface.getRoleList("1", "user"))
                .containsExactly(StpInterfaceImpl.ROLE_USER);
        assertThat(stpInterface.getRoleList("1", "rider"))
                .containsExactly(StpInterfaceImpl.ROLE_RIDER);
        assertThat(stpInterface.getPermissionList("1", "user")).isEmpty();
    }

    @Test
    void superAdminHasBothAdminRolesAndWildcardPermission() {
        when(adminMapper.selectById(1L)).thenReturn(admin(1L, "super_admin", 1));

        assertThat(stpInterface.getRoleList("1", "login"))
                .containsExactly(
                        StpInterfaceImpl.ROLE_ADMIN,
                        StpInterfaceImpl.ROLE_SUPER_ADMIN);
        assertThat(stpInterface.getPermissionList("1", "login")).containsExactly("*");
    }

    @Test
    void normalAdminOnlyHasAdminRole() {
        when(adminMapper.selectById(2L)).thenReturn(admin(2L, "admin", 1));

        assertThat(stpInterface.getRoleList("2", "login"))
                .containsExactly(StpInterfaceImpl.ROLE_ADMIN);
        assertThat(stpInterface.getPermissionList("2", "login")).isEmpty();
    }

    @Test
    void disabledOrMissingAdminHasNoRoles() {
        when(adminMapper.selectById(3L)).thenReturn(admin(3L, "super_admin", 0));
        when(adminMapper.selectById(4L)).thenReturn(null);

        assertThat(stpInterface.getRoleList("3", "login")).isEmpty();
        assertThat(stpInterface.getPermissionList("3", "login")).isEmpty();
        assertThat(stpInterface.getRoleList("4", "login")).isEmpty();
    }

    @Test
    void nonNumericAdminLoginIdHasNoRoles() {
        assertThat(stpInterface.getRoleList("abc", "login")).isEmpty();
        assertThat(stpInterface.getPermissionList("abc", "login")).isEmpty();
    }

    private Admin admin(Long id, String role, int status) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setRole(role);
        admin.setStatus(status);
        return admin;
    }
}
