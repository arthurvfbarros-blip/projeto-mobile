# Projeto Mobile

Aplicativo para contratação de serviços.

## Tecnologias

- **Backend:** Java 25, Spring Boot, Maven e Spring Data JPA.
- **Banco de dados:** PostgreSQL.

## Requisitos

- Java 25.
- Maven.
- Variáveis de conexão com o PostgreSQL configuradas.

## Variáveis de ambiente

O backend utiliza estas variáveis para conectar ao PostgreSQL:

- `DB_URL`: endereço JDBC do banco, como `jdbc:postgresql://HOST:5432/postgres?sslmode=require`.
- `DB_USERNAME`: usuário do banco.
- `DB_PASSWORD`: senha do usuário do banco.

## Executar o backend

Na raiz do projeto, execute:

```bash
cd backend
mvn spring-boot:run
```

Para verificar o funcionamento, acesse `http://localhost:8080/actuator/health`. O resultado esperado é `{"status":"UP"}`.

## Testar o backend

Dentro da pasta `backend/`, execute:

```bash
mvn test
```

## Executar o frontend

Com o backend rodando, na raiz do projeto, execute:

```bash
cd frontend
npm install
npx expo start
```

Em desenvolvimento, o app procura a API na porta `8080` da mesma máquina que roda o `expo start`, então funciona no navegador, no emulador e no celular (pelo Expo Go, na mesma rede Wi-Fi) sem configuração extra. Para usar outro endereço, crie `frontend/.env.local` com:

```bash
EXPO_PUBLIC_API_URL=http://192.168.0.10:8080
```

No navegador, a API só aceita requisições vindas de `http://localhost:8081` (variável `CORS_ALLOWED_ORIGINS` do backend).
