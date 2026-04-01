package com.silentroom.routing

import com.silentroom.dto.request.RespondInvitationRequest
import com.silentroom.dto.request.SendInvitationRequest
import com.silentroom.dto.response.ErrorResponse
import com.silentroom.dto.response.SuccessResponse
import com.silentroom.service.ContactService
import com.silentroom.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.contactRoutes(contactService: ContactService) {

    authenticate("auth-jwt") {
        route("/api/contacts") {
            get {
                val uid = UUID.fromString(call.userId())
                val contacts = contactService.getContacts(uid)
                call.respond(contacts)
            }

            get("/invitations/incoming") {
                val uid = UUID.fromString(call.userId())
                val invitations = contactService.getIncomingInvitations(uid)
                call.respond(invitations)
            }

            get("/invitations/outgoing") {
                val uid = UUID.fromString(call.userId())
                val invitations = contactService.getOutgoingInvitations(uid)
                call.respond(invitations)
            }

            post("/invite") {
                val uid = UUID.fromString(call.userId())
                val request = call.receive<SendInvitationRequest>()
                val contact = contactService.sendInvitation(uid, request.username)
                call.respond(HttpStatusCode.Created, contact)
            }

            post("/invitations/{id}/respond") {
                val uid = UUID.fromString(call.userId())
                val invitationId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Invitation ID required"))
                val request = call.receive<RespondInvitationRequest>()
                val contact = contactService.respondToInvitation(invitationId, uid, request.accept)
                call.respond(contact)
            }

            delete("/{id}") {
                val uid = UUID.fromString(call.userId())
                val contactId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Contact ID required"))
                val removed = contactService.removeContact(contactId, uid)
                if (removed) {
                    call.respond(SuccessResponse("Contact removed"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Contact not found"))
                }
            }
        }
    }
}
