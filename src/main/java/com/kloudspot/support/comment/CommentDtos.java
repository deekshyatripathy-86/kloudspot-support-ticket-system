package com.kloudspot.support.comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
public final class CommentDtos {
    private CommentDtos(){}
    public record CreateRequest(@NotBlank @Size(max=5000) String content, boolean internal){}
    public record Response(Long id,Long ticketId,Long authorId,String authorName,String content,boolean internal,Instant createdAt){}
}
