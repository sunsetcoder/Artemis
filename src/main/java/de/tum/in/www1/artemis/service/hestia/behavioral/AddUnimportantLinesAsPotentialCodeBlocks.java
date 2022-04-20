package de.tum.in.www1.artemis.service.hestia.behavioral;

import java.util.TreeSet;
import java.util.regex.Pattern;

import de.tum.in.www1.artemis.service.hestia.behavioral.GroupedFile.ChangeBlock;

public class AddUnimportantLinesAsPotentialCodeBlocks extends BehavioralKnowledgeSource {

    private static final Pattern curlyBracesPattern = Pattern.compile("\\s*}\\s*");

    private static final Pattern elsePattern = Pattern.compile("\\s*}?\\s*else\\s*\\{?\\s*");

    private static final Pattern emptyLinePattern = Pattern.compile("\\s*");

    public AddUnimportantLinesAsPotentialCodeBlocks(BehavioralBlackboard blackboard) {
        super(blackboard);
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getGroupedFiles() != null && blackboard.getGroupedFiles().stream().noneMatch(groupedFile -> groupedFile.getFileContent() == null)
                && blackboard.getGroupedFiles().stream().noneMatch(groupedFile -> groupedFile.getCommonLines() == null);
    }

    @Override
    public boolean executeAction() throws BehavioralSolutionEntryGenerationException {
        boolean didChanges = false;

        for (GroupedFile groupedFile : blackboard.getGroupedFiles()) {
            var newChangeBlocks = new TreeSet<ChangeBlock>();

            for (ChangeBlock commonChange : groupedFile.getCommonChanges()) {
                var firstLine = commonChange.getLines().first();
                var potentialPrefix = getPotentialPrefix(firstLine, groupedFile.getFileContent());
                if (potentialPrefix != null) {
                    newChangeBlocks.add(potentialPrefix);
                }
                var lastLine = commonChange.getLines().last();
                var potentialPostfix = getPotentialPostfix(lastLine, groupedFile.getFileContent());
                if (potentialPostfix != null) {
                    newChangeBlocks.add(potentialPostfix);
                }
            }

            if (!newChangeBlocks.isEmpty()) {
                groupedFile.getCommonChanges().addAll(newChangeBlocks);
                didChanges = true;
            }
        }

        return didChanges;
    }

    private ChangeBlock getPotentialPrefix(int firstLine, String fileContent) {
        var potentialLines = new TreeSet<Integer>();
        var lineContents = fileContent.split("\n");
        for (int i = firstLine - 2; i >= 0; i--) {
            var lineContent = lineContents[i];
            if (doesLineMatch(lineContent)) {
                potentialLines.add(i + 1);
            }
            else {
                break;
            }
        }
        if (potentialLines.isEmpty()) {
            return null;
        }
        else {
            return new ChangeBlock(potentialLines, true);
        }
    }

    private ChangeBlock getPotentialPostfix(int lastLine, String fileContent) {
        var potentialLines = new TreeSet<Integer>();
        var lineContents = fileContent.split("\n");
        for (int i = lastLine; i < lineContents.length; i++) {
            var lineContent = lineContents[i];
            if (doesLineMatch(lineContent)) {
                potentialLines.add(i + 1);
            }
            else {
                break;
            }
        }
        if (potentialLines.isEmpty()) {
            return null;
        }
        else {
            return new ChangeBlock(potentialLines, true);
        }
    }

    private boolean doesLineMatch(String lineContent) {
        return curlyBracesPattern.matcher(lineContent).matches() || elsePattern.matcher(lineContent).matches() || emptyLinePattern.matcher(lineContent).matches();
    }
}
