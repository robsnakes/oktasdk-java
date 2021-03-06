import com.okta.sdk.clients.UserApiClient;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.framework.PagedResults;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;
import com.okta.sdk.models.users.User;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class UserApiClientTest {

    static ApiClientConfiguration oktaSettings;
    static UserApiClient usersClient;
    static Random random;

    @BeforeClass
    public void setUp() throws Exception {
        oktaSettings = new ApiClientConfiguration(TestConstants.getUrlPath(), TestConstants.getApiKey());

        usersClient = new UserApiClient(oktaSettings);

        random = new Random();
    }

    @Test
    public void testCRUD() throws Exception {
        // Create
        User newUser = usersClient.createUser(
                "First",
                "Last",
                "login" + Integer.toString(random.nextInt()) + "@example.com",
                "email" + Integer.toString(random.nextInt()) + "@example.com",
                true);

        // Read
        User user = usersClient.getUser(newUser.getId());
        Assert.assertTrue(newUser.getId().equals(user.getId()));

        // Update
        user.getProfile().setLastName("NewLast");
        user = usersClient.updateUser(user);

        // Delete
        usersClient.deleteUser(user.getId());
    }

    @Test
    public void testLifecycles() throws Exception {
        // Create user without activating
        User newUser = usersClient.createUser(
                "First",
                "Last",
                "login" + Integer.toString(random.nextInt()) + "@example.com",
                "email" + Integer.toString(random.nextInt()) + "@example.com",
                false);

        // Activate without email
        Map result = usersClient.activateUser(newUser.getId(), false);
        Assert.assertNotNull(result.get("activationUrl"));

        // Deactivate
        Map deactivateResult = usersClient.deactivateUser(newUser.getId());
        newUser = usersClient.getUser(newUser.getId());
        Assert.assertEquals(newUser.getStatus(), "DEPROVISIONED");

        // Activate with email
        Map activateResult = usersClient.activateUser(newUser.getId());
        newUser = usersClient.getUser(newUser.getId());
        Assert.assertEquals(newUser.getStatus(), "PROVISIONED");
    }

    @Test
    public void testListUsers() throws Exception {
        List<User> users = usersClient.getUsers();
        Assert.assertTrue(users.size() > 1);
    }

    @Test
    public void testListUsersWithPagination() throws Exception {
        PagedResults<User> pagedResults = usersClient.getUsersPagedResultsWithLimit(1);

        int counter = 0;
        do {
            if(!pagedResults.isFirstPage()) {
                pagedResults = usersClient.getUsersPagedResultsByUrl(pagedResults.getNextUrl());
            }

            for(User user : pagedResults.getResult()) {
                counter++;
            }
        }
        while(!pagedResults.isLastPage());

        Assert.assertTrue(counter > 1);
    }

    @Test
    public void testSetRecoveryQuestion() throws Exception {

        // Create and activate a user
        String username = "fakeuser" + random.nextInt() + "@fake.com";
        usersClient.createUser("First", "Last", username, username, true);

        // Set their recovery question, it shouldn't throw an exception
        usersClient.setRecoveryQuestion(username, "What is your favorite color?", "Blue, no green");
    }

    @Test
    public void testSetAndChangePassword() throws Exception {

        // Create and activate a user
        String username = "fakeuser" + random.nextInt() + "@fake.com";
        User user = usersClient.createUser("First", "Last", username, username, true);

        // Set their password, it shouldn't throw an exception
        String password = "A1a!" + random.nextInt();
        usersClient.setPassword(user.getId(), password);

        // Change their password, it shouldn't throw an exception
        usersClient.changePassword(user.getId(), password, "newA1a!" + random.nextInt());
    }

    @Test
    public void testAddArbitraryValuesToProfile() throws Exception {

        // Create and activate a user
        String username = "fakeuser" + random.nextInt() + "@fake.com";
        User user = usersClient.createUser("First", "Last", username, username, true);

        // Get and modify the unmapped properties of a user
        Map<String, Object> unmappedProperties = user.getProfile().getUnmapped();
        String propName = "randomProp";
        String propVal = "value";
        unmappedProperties.put(propName, propVal);
        user = usersClient.updateUser(user);

        // Ensure the property was set and the value was stored
        unmappedProperties = user.getProfile().getUnmapped();
        Assert.assertTrue(unmappedProperties.containsKey(propName), "The unmapped property wasn't added");
        Assert.assertEquals(unmappedProperties.get(propName), propVal, "The unmapped property was set to a different value");
    }
}