package com.kloudspot.support.ticket;

import com.kloudspot.support.common.*;
import com.kloudspot.support.security.PrincipalUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService service;
    public TicketController(TicketService service) { this.service=service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TicketDtos.TicketResponse create(@Valid @RequestBody TicketDtos.CreateRequest req) {
        return service.create(req,PrincipalUtil.current());
    }
    @GetMapping public Page<TicketDtos.TicketResponse> list(
            @RequestParam(required=false) TicketStatus status,
            @RequestParam(required=false) TicketPriority priority,
            @RequestParam(required=false) Long assignedAgentId,
            @RequestParam(required=false) String search,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(defaultValue="createdAt") String sortBy,
            @RequestParam(defaultValue="DESC") String sortDir) {
        return service.list(PrincipalUtil.current(),status,priority,assignedAgentId,search,page,size,sortBy,sortDir);
    }
    @GetMapping("/{id}") public TicketDtos.TicketResponse get(@PathVariable Long id) { return service.get(id,PrincipalUtil.current()); }
    @PatchMapping("/{id}") public TicketDtos.TicketResponse update(@PathVariable Long id,@Valid @RequestBody TicketDtos.UpdateRequest req) {
        return service.update(id,req,PrincipalUtil.current());
    }
    @PatchMapping("/{id}/assign") public TicketDtos.TicketResponse assign(@PathVariable Long id,@Valid @RequestBody TicketDtos.AssignRequest req) {
        return service.assign(id,req,PrincipalUtil.current());
    }
    @PatchMapping("/{id}/status") public TicketDtos.TicketResponse status(@PathVariable Long id,@Valid @RequestBody TicketDtos.StatusRequest req) {
        return service.changeStatus(id,req,PrincipalUtil.current());
    }
    @PatchMapping("/{id}/close") public TicketDtos.TicketResponse close(@PathVariable Long id) { return service.close(id,PrincipalUtil.current()); }
    @GetMapping("/{id}/history") public java.util.List<TicketDtos.HistoryResponse> history(@PathVariable Long id) {
        return service.getHistory(id,PrincipalUtil.current());
    }
}
