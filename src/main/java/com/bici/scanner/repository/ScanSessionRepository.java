package com.bici.scanner.repository;

import com.bici.scanner.exception.DatabaseException;
import com.bici.scanner.repository.entity.ScanSession;
import com.bici.scanner.repository.entity.ScanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScanSessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(ScanSessionRepository.class);
    private final Connection connection;

    public ScanSessionRepository(Connection connection) {
        this.connection = connection;
    }

    public void save(ScanSession session) throws DatabaseException {
        String sql = """
            INSERT INTO scan_sessions 
            (scan_id, status, scanner_name, options_json, created_at, started_at, 
             completed_at, cancelled_at, error_message, pdf_path, pdf_size, created_at_iso)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, session.getScanId());
            stmt.setString(2, session.getStatus().name());
            stmt.setString(3, session.getScannerName());
            stmt.setString(4, session.getOptionsJson());
            stmt.setLong(5, session.getCreatedAt());
            setLongOrNull(stmt, 6, session.getStartedAt());
            setLongOrNull(stmt, 7, session.getCompletedAt());
            setLongOrNull(stmt, 8, session.getCancelledAt());
            stmt.setString(9, session.getErrorMessage());
            stmt.setString(10, session.getPdfPath());
            setLongOrNull(stmt, 11, session.getPdfSize());
            stmt.setString(12, session.getCreatedAtIso());
            
            stmt.executeUpdate();
            logger.debug("Session de scan sauvegardée: {}", session.getScanId());
        } catch (SQLException e) {
            logger.error("Erreur lors de la sauvegarde de la session", e);
            throw new DatabaseException("Erreur sauvegarde session", e);
        }
    }

    public Optional<ScanSession> findById(String scanId) throws DatabaseException {
        String sql = "SELECT * FROM scan_sessions WHERE scan_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, scanId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de la session: {}", scanId, e);
            throw new DatabaseException("Erreur recherche session", e);
        }
    }

    public void update(ScanSession session) throws DatabaseException {
        String sql = """
            UPDATE scan_sessions SET
                status = ?, scanner_name = ?, options_json = ?,
                started_at = ?, completed_at = ?, cancelled_at = ?,
                error_message = ?, pdf_path = ?, pdf_size = ?
            WHERE scan_id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, session.getStatus().name());
            stmt.setString(2, session.getScannerName());
            stmt.setString(3, session.getOptionsJson());
            setLongOrNull(stmt, 4, session.getStartedAt());
            setLongOrNull(stmt, 5, session.getCompletedAt());
            setLongOrNull(stmt, 6, session.getCancelledAt());
            stmt.setString(7, session.getErrorMessage());
            stmt.setString(8, session.getPdfPath());
            setLongOrNull(stmt, 9, session.getPdfSize());
            stmt.setString(10, session.getScanId());
            
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Session non trouvée pour mise à jour: " + session.getScanId());
            }
            logger.debug("Session de scan mise à jour: {}", session.getScanId());
        } catch (SQLException e) {
            logger.error("Erreur lors de la mise à jour de la session", e);
            throw new DatabaseException("Erreur mise à jour session", e);
        }
    }

    public List<ScanSession> findAll(int limit, int offset, ScanStatus statusFilter) throws DatabaseException {
        List<ScanSession> sessions = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM scan_sessions"
        );
        
        if (statusFilter != null) {
            sql.append(" WHERE status = ?");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (statusFilter != null) {
                stmt.setString(paramIndex++, statusFilter.name());
            }
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération de l'historique", e);
            throw new DatabaseException("Erreur récupération historique", e);
        }
        
        return sessions;
    }

    public int count(ScanStatus statusFilter) throws DatabaseException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM scan_sessions");
        
        if (statusFilter != null) {
            sql.append(" WHERE status = ?");
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            if (statusFilter != null) {
                stmt.setString(1, statusFilter.name());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du comptage des sessions", e);
            throw new DatabaseException("Erreur comptage sessions", e);
        }
    }

    public Optional<ScanSession> findRunningScan() throws DatabaseException {
        String sql = "SELECT * FROM scan_sessions WHERE status = ? LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ScanStatus.RUNNING.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche d'un scan en cours", e);
            throw new DatabaseException("Erreur recherche scan en cours", e);
        }
    }

    private ScanSession mapResultSetToSession(ResultSet rs) throws SQLException {
        ScanSession session = new ScanSession();
        session.setScanId(rs.getString("scan_id"));
        session.setStatus(ScanStatus.valueOf(rs.getString("status")));
        session.setScannerName(rs.getString("scanner_name"));
        session.setOptionsJson(rs.getString("options_json"));
        session.setCreatedAt(rs.getLong("created_at"));
        session.setStartedAt(getLongOrNull(rs, "started_at"));
        session.setCompletedAt(getLongOrNull(rs, "completed_at"));
        session.setCancelledAt(getLongOrNull(rs, "cancelled_at"));
        session.setErrorMessage(rs.getString("error_message"));
        session.setPdfPath(rs.getString("pdf_path"));
        session.setPdfSize(getLongOrNull(rs, "pdf_size"));
        session.setCreatedAtIso(rs.getString("created_at_iso"));
        return session;
    }

    private void setLongOrNull(PreparedStatement stmt, int index, Long value) throws SQLException {
        if (value != null) {
            stmt.setLong(index, value);
        } else {
            stmt.setNull(index, Types.BIGINT);
        }
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}

