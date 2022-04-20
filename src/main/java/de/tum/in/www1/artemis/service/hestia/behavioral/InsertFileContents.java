package de.tum.in.www1.artemis.service.hestia.behavioral;

public class InsertFileContents extends BehavioralKnowledgeSource {

    public InsertFileContents(BehavioralBlackboard blackboard) {
        super(blackboard);
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getGroupedFiles() != null && blackboard.getGroupedFiles().stream().anyMatch(groupedFile -> groupedFile.getFileContent() == null);
    }

    @Override
    public boolean executeAction() throws BehavioralSolutionEntryGenerationException {
        for (GroupedFile groupedFile : blackboard.getGroupedFiles()) {
            var fileContent = blackboard.getSolutionRepoFiles().get(groupedFile.getFilePath());
            if (fileContent == null) {
                throw new BehavioralSolutionEntryGenerationException(
                        String.format("Unable to find file '%s' in the solution repo despite it being referenced in the git-diff and coverage", groupedFile.getFilePath()));
            }
            groupedFile.setFileContent(fileContent);
        }
        return !blackboard.getGroupedFiles().isEmpty();
    }
}
