package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.domain.notification.SystemNotification;
import de.tum.in.www1.artemis.repository.SystemNotificationRepository;
import de.tum.in.www1.artemis.util.ModelFactory;

public class SystemNotificationIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    private SystemNotificationRepository systemNotificationRepo;

    private SystemNotification systemNotification;

    private SystemNotification systemNotificationActive;

    @BeforeEach
    public void initTestCase() {
        // Generate a system notification that has expired.
        SystemNotification systemNotificationExpired = ModelFactory.generateSystemNotification(ZonedDateTime.now().minusDays(8), ZonedDateTime.now().minusMinutes(25));
        systemNotificationRepo.save(systemNotificationExpired);

        // Generate a system notification whose notification date is in the future.
        SystemNotification systemNotificationFuture = ModelFactory.generateSystemNotification(ZonedDateTime.now().plusMinutes(25), ZonedDateTime.now().plusDays(8));
        systemNotificationRepo.save(systemNotificationFuture);

        // Generate an active system notification
        systemNotificationActive = ModelFactory.generateSystemNotification(ZonedDateTime.now().minusMinutes(25), ZonedDateTime.now().plusMinutes(25));
        systemNotificationRepo.save(systemNotificationActive);

        systemNotification = ModelFactory.generateSystemNotification(ZonedDateTime.now().minusDays(3), ZonedDateTime.now().plusDays(3));
    }

    @AfterEach
    public void resetDatabase() {
        systemNotificationRepo.deleteAll();
    }

    @Test
    @WithAnonymousUser
    public void testGetActiveSystemNotificationWithAnonymousUser() throws Exception {
        // Do the actual request that is tested here.
        getActiveSystemNotification();
    }

    @Test
    public void testGetActiveSystemNotificationWithoutUser() throws Exception {
        getActiveSystemNotification();
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetActiveSystemNotification() throws Exception {
        // Do the actual request that is tested here.
        getActiveSystemNotification();
    }

    private void getActiveSystemNotification() throws Exception {
        // Do the actual request that is tested here.
        SystemNotification notification = request.get("/api/system-notifications/active-notification", HttpStatus.OK, SystemNotification.class);

        // The returned notification must be an active notification.
        assertThat(systemNotification.getExpireDate()).as("Returned notification has not expired yet.").isAfterOrEqualTo(ZonedDateTime.now());
        assertThat(systemNotification.getNotificationDate()).as("Returned notification is active.").isBeforeOrEqualTo(ZonedDateTime.now());
        assertThat(notification).as("Returned notification is active system notification.").isEqualTo(systemNotificationActive);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testCreateSystemNotification() throws Exception {
        SystemNotification response = request.postWithResponseBody("/api/system-notifications", systemNotification, SystemNotification.class);

        assertThat(systemNotificationRepo.findById(response.getId())).as("Saved system notification").isNotNull();
        assertThat(systemNotificationRepo.existsById(response.getId())).as("Saved notification exists").isTrue();
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testCreateSystemNotification_BadRequest() throws Exception {
        systemNotification.setId(1L);
        request.post("/api/system-notifications", systemNotification, HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "{displayName} [{index}] {argumentsWithNames}")
    @ValueSource(strings = { "/api/system-notifications/", "/api/notifications/" })
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testCreateSystemNotification_asInstructor_Forbidden(String endpoint) throws Exception {
        request.post(endpoint, systemNotification, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testCreateSystemNotification_asUser_Forbidden() throws Exception {
        systemNotification.setId(1L);
        request.post("/api/system-notifications", systemNotification, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testUpdateSystemNotification() throws Exception {
        systemNotificationRepo.save(systemNotification);
        String updatedText = "updated text";
        systemNotification.setText(updatedText);
        SystemNotification response = request.putWithResponseBody("/api/system-notifications", systemNotification, SystemNotification.class, HttpStatus.OK);
        assertThat(response.getText()).as("response has updated text").isEqualTo(updatedText);
        assertThat(systemNotificationRepo.findById(systemNotification.getId())).get().as("repository contains updated notification").isEqualTo(response);
    }

    @ParameterizedTest(name = "{displayName} [{index}] {argumentsWithNames}")
    @ValueSource(strings = { "/api/system-notifications/", "/api/notifications/" })
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateSystemNotification_asInstructor_Forbidden(String endpoint) throws Exception {
        systemNotificationRepo.save(systemNotification);
        systemNotification.setText("updated text");
        request.put(endpoint, systemNotification, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testUpdateSystemNotification_BadRequest() throws Exception {
        SystemNotification systemNotification = ModelFactory.generateSystemNotification(ZonedDateTime.now().minusDays(3), ZonedDateTime.now().plusDays(3));
        request.put("/api/system-notifications", systemNotification, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testGetAllSystemNotifications() throws Exception {
        List<SystemNotification> response = request.getList("/api/system-notifications", HttpStatus.OK, SystemNotification.class);
        assertThat(response).as("system notification are present").isNotEmpty();
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testGetSystemNotification() throws Exception {
        SystemNotification response = request.postWithResponseBody("/api/system-notifications", systemNotification, SystemNotification.class);
        assertThat(systemNotificationRepo.findById(response.getId())).get().as("system notification is not null").isNotNull();
        request.get("/api/system-notifications/" + response.getId(), HttpStatus.OK, SystemNotification.class);
        request.get("/api/system-notifications/" + response.getId() + 1, HttpStatus.NOT_FOUND, SystemNotification.class);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    public void testDeleteSystemNotification() throws Exception {
        SystemNotification response = request.postWithResponseBody("/api/system-notifications", systemNotification, SystemNotification.class);
        assertThat(systemNotificationRepo.findById(response.getId())).get().as("system notification is not null").isNotNull();
        request.delete("/api/system-notifications/" + response.getId(), HttpStatus.OK);
    }

    @ParameterizedTest(name = "{displayName} [{index}] {argumentsWithNames}")
    @ValueSource(strings = { "/api/system-notifications/", "/api/notifications/" })
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteSystemNotification_asInstructor_Forbidden(String endpoint) throws Exception {
        SystemNotification notification = systemNotificationRepo.save(systemNotification);
        assertThat(systemNotificationRepo.findById(notification.getId())).get().as("system notification is not null").isNotNull();
        request.delete(endpoint + notification.getId(), HttpStatus.FORBIDDEN);
    }
}
