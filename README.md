# Projeto Mobile

Aplicativo para contratação de serviços, com cadastro de clientes e prestadores, endereços, pedidos, orçamentos, agendamento, chat em tempo real e confirmação da contratação.

## Tecnologias

- **Frontend:** React Native, Expo, TypeScript, React Native Web e Expo Router.
- **Backend:** Java 25, Spring Boot, Maven, JPA/Hibernate e Spring Security com JWT.
- **Banco:** PostgreSQL no Supabase.
- **Tempo real:** WebSocket com STOMP.

O código está dividido em `frontend/` e `backend/`, com o backend organizado por funcionalidade.

## Requisitos

- JDK 25 e Maven (ou `./mvnw` em `backend/`).
- Node.js 24.3 ou superior da série 24, com npm.
- PostgreSQL com o esquema da aplicação já configurado. O backend valida as tabelas, sem criá-las ou atualizá-las automaticamente.

## Configuração

Exporte as variáveis no terminal que executará o backend:

```bash
export DB_URL='jdbc:postgresql://HOST:5432/postgres?sslmode=require'
export DB_USERNAME='USUARIO_DO_BANCO'
export DB_PASSWORD='SENHA_DO_BANCO'
export JWT_SECRET='SUA_CHAVE_ALEATORIA_COM_PELO_MENOS_32_BYTES'
```

Gere uma chave com `openssl rand -hex 32` e use o resultado em `JWT_SECRET`. 

## Execução

Backend, a partir da raiz:

```bash
cd backend
mvn spring-boot:run
```

API em `http://localhost:8080`. Verifique a conexão em `/actuator/health`; o retorno esperado é `{"status":"UP"}`.

Frontend, em outro terminal a partir da raiz:

```bash
cd frontend
npm ci
npm run web
```

Aplicativo em `http://localhost:8081`. Para outras plataformas, use `npm start`.

Para acessar pelo navegador do celular, defina `EXPO_PUBLIC_API_URL=http://IP_DO_COMPUTADOR:8080` em `frontend/.env.local` e permita a origem `http://IP_DO_COMPUTADOR:8081` na variável `CORS_ALLOWED_ORIGINS` do backend. Por padrão, as origens permitidas são `http://localhost:8081,http://127.0.0.1:8081`. Reinicie os serviços após alterar as configurações.

## Verificações

Dentro de `backend/`:

```bash
mvn test
```

Os testes usam H2 em memória.

Dentro de `frontend/`:

```bash
npm run lint
npx tsc --noEmit
```

Para testar o fluxo, use sessões separadas de cliente e prestador. O prestador precisa estar aprovado no banco. Siga: pedido → orçamento → agendamento → mensagem de cada participante → confirmação da contratação.
