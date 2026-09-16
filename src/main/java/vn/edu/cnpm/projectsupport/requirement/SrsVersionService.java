package vn.edu.cnpm.projectsupport.requirement;

import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;

@Service
public class SrsVersionService {
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    public SrsVersionService(JdbcTemplate jdbc, CurrentUserService currentUserService, ObjectMapper objectMapper) {
        this.jdbc=jdbc; this.currentUserService=currentUserService; this.objectMapper=objectMapper;
    }

    @Transactional
    public Map<String,Object> generate(Long projectId) {
        Long userId=currentUserService.findCurrentUser().orElseThrow().getId();
        List<Map<String,Object>> requirements=jdbc.queryForList("SELECT jira_issue_key,title,description,actor,priority,precondition,main_flow,alternative_flow,exception_flow,postcondition,status FROM requirements WHERE project_id=? ORDER BY id",projectId);
        long next=jdbc.queryForObject("SELECT COUNT(*)+1 FROM srs_versions WHERE project_id=?",Long.class,projectId);
        String version="v"+next;
        String checksum=checksum(requirements);
        String content=json(requirements);
        jdbc.update("INSERT INTO srs_versions(project_id,version,generated_by_user_id,generated_at,source_synced_at,file_url,checksum,content_json) VALUES(?,?,?,CURRENT_TIMESTAMP(6),?, ?,?,?)",
                projectId,version,userId,Instant.now(),"/api/v1/projects/"+projectId+"/srs/versions/"+version,checksum,content);
        return document(projectId,version,requirements,checksum);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> versions(Long projectId) {
        return jdbc.queryForList("SELECT version,generated_by_user_id,generated_at,source_synced_at,file_url,checksum FROM srs_versions WHERE project_id=? ORDER BY generated_at DESC",projectId);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> version(Long projectId,String version) {
        Map<String,Object> row=jdbc.queryForMap("SELECT checksum,content_json FROM srs_versions WHERE project_id=? AND version=?",projectId,version);
        try {
            @SuppressWarnings("unchecked") List<Map<String,Object>> requirements=objectMapper.readValue(String.valueOf(row.get("content_json")),List.class);
            return document(projectId,version,requirements,String.valueOf(row.get("checksum")));
        } catch(Exception e){throw new IllegalStateException("Không đọc được phiên bản SRS",e);}
    }

    private Map<String,Object> document(Long projectId,String version,List<Map<String,Object>> requirements,String checksum){
        return Map.of("projectId",projectId,"version",version,"title","Software Requirements Specification",
                "generatedAt",Instant.now(),"checksum",checksum,"requirements",requirements);
    }
    private String checksum(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Không tạo được checksum SRS",e);}}
    private String json(Object value){try{return objectMapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Không tạo được nội dung SRS",e);}}
}
