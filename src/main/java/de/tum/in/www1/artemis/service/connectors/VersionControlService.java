package de.tum.in.www1.artemis.service.connectors;

import java.net.URL;
import java.util.Set;

import de.tum.in.www1.artemis.domain.Commit;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.VcsRepositoryUrl;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.exception.VersionControlException;

public interface VersionControlService {

    /**
     * Configure the repository for the student(s), this mainly includes setting permissions for the passed users
     *
     * @param exercise the programming exercise for which the repository should be configured
     * @param repositoryUrl the url of the repository that needs to be configured
     * @param users one user in an individual exercise, multiple users for a team exercise
     * @param allowAccess this determines if the users should get access to the repository directly. You normally want this to be true.
     */
    void configureRepository(ProgrammingExercise exercise, URL repositoryUrl, Set<User> users, boolean allowAccess);

    /**
     * Creates all necessary webhooks from the VCS to any other system (e.g. Artemis, CI) on pushes to the specified
     * repository.
     *
     * @param exercise The programming exercise for which to add all required webhooks
     */
    void addWebHooksForExercise(ProgrammingExercise exercise);

    /**
     * Adds the webhook for new pushes to a participation repository to Artemis
     *
     * @param participation The participation for which webhooks should get triggered
     */
    void addWebHookForParticipation(ProgrammingExerciseParticipation participation);

    /**
     * Deletes the project for the given project key
     *
     * @param projectKey of the project that should be deleted
     */
    void deleteProject(String projectKey);

    /**
     * Deletes the repository at the given url
     *
     * @param repositoryUrl of the repository that should be deleted
     */
    void deleteRepository(URL repositoryUrl);

    /**
     * Get the clone URL used for cloning
     *
     * @param projectKey     The project key
     * @param repositorySlug The repository slug
     * @return The clone URL
     */
    VcsRepositoryUrl getCloneRepositoryUrl(String projectKey, String repositorySlug);

    /**
     * Check if the given repository url is valid and accessible.
     *
     * @param repositoryUrl repository URL
     * @return whether the repository is valid
     */
    Boolean repositoryUrlIsValid(URL repositoryUrl);

    /**
     * Get the last commit details that are included in the given requestBody that notifies about a push
     *
     * @param requestBody The request Body received from the VCS.
     * @return the last commit details that are included in the given requestBody
     * @throws VersionControlException if the Body could not be parsed
     */
    Commit getLastCommitDetails(Object requestBody) throws VersionControlException;

    /**
     * Creates a project on the VCS.
     *
     * @param programmingExercise for which a project should be created
     * @throws VersionControlException if the project could not be created
     */
    void createProjectForExercise(ProgrammingExercise programmingExercise) throws VersionControlException;

    /**
     * Creates a repository on the VCS.
     *
     * @param repoName         The name of repository
     * @param projectKey       The key of the project that contains the repository (must exist)
     * @param parentProjectKey The key of parent project (for sub-groups in Gitlab), null if not applicable
     * @throws VersionControlException if the repository could not be created
     */
    void createRepository(String projectKey, String repoName, String parentProjectKey) throws VersionControlException;

    /**
     * Gets the repository name of a given repository url
     *
     * @param repositoryUrl The repository url
     * @return The repository name
     */
    String getRepositoryName(URL repositoryUrl);

    /**
     * Checks if the project with the given projectKey already exists
     *
     * @param projectKey to check if a project with this unique key already exists
     * @param projectName to check if a project with the same name already exists
     * @return true if the project exists, false otherwise
     */
    boolean checkIfProjectExists(String projectKey, String projectName);

    /**
     * Copies a repository from one project to another one. The project can be the same.
     *
     * @param sourceProjectKey The key of the template project (normally based on the course and exercise short name)
     * @param sourceRepositoryName The name of the repository which should be copied
     * @param targetProjectKey The key of the target project to which to copy the new plan to
     * @param targetRepositoryName The desired name of the target repository
     * @return The URL for cloning the repository
     * @throws VersionControlException if the repository could not be copied on the VCS server (e.g. because the source repo does not exist)
     */
    VcsRepositoryUrl copyRepository(String sourceProjectKey, String sourceRepositoryName, String targetProjectKey, String targetRepositoryName) throws VersionControlException;

    /**
     * Add the user to the repository
     *
     * @param repositoryUrl     The repository url of the repository to which to add the user. It contains the project key & the repository name.
     * @param user              User which to add to the repository
     */
    void addMemberToRepository(URL repositoryUrl, User user);

    /**
     * Remove the user from the repository
     *
     * @param repositoryUrl     The repository url of the repository from which to remove the user. It contains the project key & the repository name.
     * @param user              User which to remove from the repository
     */
    void removeMemberFromRepository(URL repositoryUrl, User user);

    /**
     * Removes the user's write permissions for a repository.
     *
     * @param repositoryUrl     The repository url of the repository to update. It contains the project key & the repository name.
     * @param projectKey        The projectKey that the repo is part of in the VCS.
     * @param users             Set of users for which to change permissions
     * @throws VersionControlException        If the communication with the VCS fails.
     */
    void setRepositoryPermissionsToReadOnly(URL repositoryUrl, String projectKey, Set<User> users) throws VersionControlException;

    /**
     * Checks if the underlying VCS server is up and running and gives some additional information about the running
     * services if available
     *
     * @return The health of the VCS service containing if it is up and running and any additional data, or the throwing exception otherwise
     */
    ConnectorHealth health();
}
