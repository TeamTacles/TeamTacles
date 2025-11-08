package br.com.teamtacles.authentication.controller;

import br.com.teamtacles.authentication.dto.request.ResetPasswordWebDTO;
import br.com.teamtacles.authentication.service.AuthenticationService;
import br.com.teamtacles.user.model.User;
import br.com.teamtacles.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/forgot-password-web")
public class ResetPasswordWebController {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public ResetPasswordWebController(UserRepository userRepository, AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        Optional<User> userOpt = userRepository.findByResetPasswordToken(token);

        if (userOpt.isEmpty() || userOpt.get().getResetPasswordTokenExpiry().isBefore(OffsetDateTime.now())) {
            model.addAttribute("error", "Este link de redefinição é inválido ou já expirou. Por favor, solicite um novo.");
            model.addAttribute("expired", true);
            return "reset-password-form";
        }

        model.addAttribute("token", token);
        model.addAttribute("email", userOpt.get().getEmail());
        model.addAttribute("form", new ResetPasswordWebDTO());
        return "reset-password-form";
    }

    @PostMapping
    public String processResetPassword(
            @Valid @ModelAttribute("form") ResetPasswordWebDTO form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model)
    {
        // Valida se senhas coincidem
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "As senhas não coincidem.");
        }

        // Se houver erros de validação, volta para o formulário
        if (bindingResult.hasErrors()) {
            User user = userRepository.findByResetPasswordToken(form.getToken()).orElse(null);
            if (user != null) {
                model.addAttribute("email", user.getEmail());
            }
            model.addAttribute("token", form.getToken());
            return "reset-password-form";
        }

        // Tenta resetar a senha
        try {
            authenticationService.resetPassword(form.getToken(), form.getNewPassword());
            return "redirect:/forgot-password-web/success";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/forgot-password-web?token=" + form.getToken();
        }
    }

    @GetMapping("/success")
    public String showSuccessPage() {
        return "reset-password-success";
    }
}