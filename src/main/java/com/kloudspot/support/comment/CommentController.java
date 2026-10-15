package com.kloudspot.support.comment;

import com.kloudspot.support.security.PrincipalUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController {
    private final CommentService service;
    public CommentController(CommentService service){this.service=service;}
    @GetMapping public List<CommentDtos.Response> list(@PathVariable Long ticketId){return service.list(ticketId,PrincipalUtil.current());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CommentDtos.Response add(@PathVariable Long ticketId,@Valid @RequestBody CommentDtos.CreateRequest req){
        return service.add(ticketId,req,PrincipalUtil.current());
    }
}
