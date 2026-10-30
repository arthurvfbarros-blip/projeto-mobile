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
