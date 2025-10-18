-- Adiciona a coluna para armazenar o token de redefinição de senha.
ALTER TABLE users ADD COLUMN reset_password_token VARCHAR(255);

-- Adiciona a coluna para armazenar a data de expiração do token.
ALTER TABLE users ADD COLUMN reset_password_token_expiry TIMESTAMP WITH TIME ZONE;