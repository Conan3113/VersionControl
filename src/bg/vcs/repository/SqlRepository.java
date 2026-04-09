package bg.vcs.repository;

import bg.vcs.model.*;
import java.sql.*;
import java.util.*;

public class SqlRepository {
    private final String URL = "jdbc:mysql://localhost:3306/vcs_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private final String USER = "root";
    private final String PASS = "conan3113";

    public void syncDocument(Document doc) {
        System.out.println("[SQL] Опит за запис на документ: " + doc.getId());

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);

            try (PreparedStatement psD = conn.prepareStatement(
                    "INSERT INTO documents (doc_id, title) VALUES (?, ?) ON DUPLICATE KEY UPDATE title = ?");
                 PreparedStatement psDel = conn.prepareStatement(
                         "DELETE FROM document_versions WHERE doc_id = ?");
                 PreparedStatement psV = conn.prepareStatement(
                         "INSERT INTO document_versions (doc_id, v_number, content, author_name, status, reviewer_comment) VALUES (?, ?, ?, ?, ?, ?)")) {


                psD.setString(1, doc.getId());
                psD.setString(2, doc.getTitle());
                psD.setString(3, doc.getTitle());
                psD.executeUpdate();


                psDel.setString(1, doc.getId());
                psDel.executeUpdate();


                for (Version v : doc.getVersions()) {
                    psV.setString(1, doc.getId());
                    psV.setInt(2, v.getId());
                    psV.setString(3, v.getContent());
                    psV.setString(4, v.getAuthorName());
                    psV.setString(5, v.getStatus().name());
                    psV.setString(6, v.getReviewerComment());
                    psV.executeUpdate();
                }


                conn.commit();
                System.out.println("[SQL] УСПЕХ: Документът е записан в MySQL!");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[SQL] ГРЕШКА в транзакцията: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.err.println("[SQL] ГРЕШКА при свързване: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Document> loadAll() {
        Map<String, Document> data = new HashMap<>();
        String sql = "SELECT d.doc_id, d.title, v.v_number, v.content, v.author_name, v.status, v.reviewer_comment " +
                "FROM documents d LEFT JOIN document_versions v ON d.doc_id = v.doc_id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("doc_id");
                Document doc = data.computeIfAbsent(id, k -> {
                    try { return new Document(id, rs.getString("title")); }
                    catch (SQLException e) { return null; }
                });
                int vNum = rs.getInt("v_number");
                if (vNum > 0) {
                    Version v = new Version(vNum, rs.getString("content"), rs.getString("author_name"));
                    v.setStatus(Status.valueOf(rs.getString("status")));
                    v.setReviewerComment(rs.getString("reviewer_comment"));
                    doc.getVersions().add(v);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }
}