package br.com.teamtacles.user.controller;

import br.com.teamtacles.common.dto.response.MessageResponseDTO;
import br.com.teamtacles.common.exception.ErrorResponse;
import br.com.teamtacles.common.exception.ResourceNotFoundException;
import br.com.teamtacles.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/verify-account") // URL base (sem /api)
@Tag(name = "User Management (Web)", description = "Endpoints for user account verification.")
public class AccountVerificationWebController {

    private final UserService userService;

    public AccountVerificationWebController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Verify user account", description = "Activates a user's account using a verification token sent via email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account verified successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Token not found, invalid, or expired",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public String verifyAccount(@RequestParam("token") String token, Model model) {
        try {
            // Tenta verificar o usuário. O service lança exceção se o token for inválido/expirado
            userService.verifyUser(token);

            // Se for bem-sucedido, renderiza a página de sucesso
            return "account-verified";

        } catch (ResourceNotFoundException e) {
            // Se o token for inválido, captura o erro e o envia para a página
            model.addAttribute("error", e.getMessage());
            return "account-verified"; // Renderiza a *mesma* página, mas agora com a mensagem de erro

        } catch (RuntimeException e) {
            // Um catch-all para outros erros inesperados
            model.addAttribute("error", "Ocorreu um erro inesperado. Tente novamente mais tarde.");
            return "account-verified";
        }
    }
}