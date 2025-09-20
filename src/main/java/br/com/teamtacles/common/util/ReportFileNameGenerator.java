package br.com.teamtacles.common.util;

import br.com.teamtacles.project.model.Project;

public final class ReportFileNameGenerator {

    private ReportFileNameGenerator() {}

    public static String gerenateForProject(Project project) {

        String newFileName = project.getTitle()
                .replaceAll("[^a-zA-Z0-9.-]", "_")
                .toLowerCase();

        return String.format("relatorio_%s.pdf", newFileName);
    }
}
