package com.codereview;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeReviewAction extends AnAction {

    private static final String GEMINI_API_KEY = "";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            Messages.showMessageDialog(project, "코드를 선택하세요.", "Error", Messages.getErrorIcon());
            return;
        }

        // 선택된 코드 가져오기
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        if (selectedText == null || selectedText.isEmpty()) {
            Messages.showMessageDialog(project, "코드를 선택하세요.", "Error", Messages.getErrorIcon());
            return;
        }

        // 팝업 창 열기 (선택된 코드 전달)
        CodeReviewDialog dialog = new CodeReviewDialog(project, selectedText);
        dialog.show();
    }

    // 팝업 다이얼로그 클래스
    private static class CodeReviewDialog extends DialogWrapper {
        private final Project project;
        private final String selectedCode;
        private JTextArea questionField;

        protected CodeReviewDialog(@Nullable Project project, String selectedCode) {
            super(project);
            this.project = project;
            this.selectedCode = selectedCode;
            setTitle("AI 코드 리뷰");
            init(); // 다이얼로그 초기화
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());

            // 선택된 코드 표시
            JTextArea codeArea = new JTextArea(selectedCode);
            codeArea.setEditable(false);
            codeArea.setLineWrap(true);
            codeArea.setWrapStyleWord(true);
            panel.add(new JScrollPane(codeArea), BorderLayout.CENTER);

            // 질문 입력 필드
            questionField = new JTextArea(3, 50);
            questionField.setBorder(BorderFactory.createTitledBorder("질문 입력"));
            panel.add(questionField, BorderLayout.SOUTH);

            return panel;
        }

        @Override
        protected JComponent createSouthPanel() {
            JPanel buttonPanel = new JPanel();
            JButton sendButton = new JButton("전송");
            sendButton.addActionListener(e -> {
                String question = questionField.getText();
                if (question.isEmpty()) {
                    Messages.showMessageDialog(project, "질문을 입력하세요.", "Error", Messages.getErrorIcon());
                    return;
                }

                // AI 리뷰 요청
                String response = getAIReview(selectedCode, question);

                // result 표시
                Messages.showMessageDialog(project, response, "AI Review", Messages.getInformationIcon());
            });

            buttonPanel.add(sendButton);
            return buttonPanel;
        }

        private String getAIReview(String selectedText, String question) {
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", "당신은 Java Spring Boot 전문가입니다. 코드를 분석하고 리뷰를 작성하세요. 모든 응답은 반드시 한국어로 작성하세요. \n\n" + selectedText + question);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);


            // Gson을 사용하여 JSON 변환
            Gson gson = new Gson();
            String json = gson.toJson(requestBody);

            // 요청 생성
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application-json")
                    .addHeader("x-goog-api-key", GEMINI_API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "API 요청 실패: " + response.code();
                }
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return "API 호출 중 오류 발생: " + e.getMessage();
            }
        }
    }
}
