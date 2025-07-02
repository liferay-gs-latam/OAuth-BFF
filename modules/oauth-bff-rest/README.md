# OAuth BFF REST Module for Liferay

This project implements a Liferay module that acts as a **Backend-for-Frontend (BFF)** to simplify communication with external APIs protected by OAuth 2.0 or OIDC.

The main goal is to abstract the complexity of obtaining and managing access tokens by providing a secure proxy endpoint that the frontend can call without needing to handle OAuth flows directly.


## Key Features

- **Centralized OAuth Client Configuration**: Manage credentials and settings for different OAuth clients (external services) through a Control Panel interface, using Liferay's "Objects" feature.
- **REST Proxy Endpoint**: A single endpoint that receives calls from the frontend, obtains the necessary access token behind the scenes, and forwards the request to the target API.
- **Token Acquisition Strategies (Token Resolvers)**:
  - **Client Credentials**: For machine-to-machine (M2M) communication.
  - **OIDC Session**: Reuses the session of a user already authenticated in Liferay via OIDC to obtain a token on behalf of that user.
  - An extensible architecture to add new strategies in the future (e.g., Authorization Code).
- **Token Caching**: Caches access tokens to improve performance and avoid unnecessary requests to the identity provider.
- **Control Panel Integration**: Allows administrators to configure OAuth clients and view tokens without needing to edit configuration files.
- **Allowed Endpoint Validation**: The BFF validates whether the `proxyPath` in the request starts with one of the explicitly allowed paths configured for each OAuthClient.
  - Leading slashes are automatically normalized for both the configuration and the requested path.
  - Example configuration for `allowedEndpoints`:
    ```
    me
    api/v1/orders
    /secure/path
    ```

## Architecture and How It Works

The request flow is as follows:

1. **Configuration**: An administrator registers a new `OAuth Client` in the Liferay Control Panel (e.g., `Control Panel > OAuth BFF > Clients`). The saved information includes `Client ID`, `Client Secret`, `Token Endpoint URL`, and the `Grant Type` (the strategy to be used).

2. **Frontend Request**: The frontend code (e.g., a React widget or JavaScript) makes a call to the BFF's proxy endpoint instead of calling the external API directly.

```javascript
// Example of a frontend call
const response = await Liferay.Util.fetch(
  '/o/oauth-bff/proxy/{alias}/api/external-data'
);
```

The `/o/oauth-bff/proxy/...` endpoint is protected by Liferay's default authentication mechanisms. This means that only authenticated users can access it.
When calling the proxy from the frontend, make sure to use `Liferay.Util.fetch()` or include the `p_auth` token in your request.


3. **BFF Processing**:
   - The `OAuthProxyController` receives the request.
   - It uses the `OAuthClientResolver` to load the OAuth client data based on the `{alias}` provided in the URL.
   - The `TokenResolverRegistry` selects the correct strategy (`TokenResolver`) based on the `Grant Type` configured for that client.
   - The `TokenResolver` obtains the token, first by checking the `OAuthTokenCacheService`. If there is no valid token in the cache, it makes a new request to the identity provider.
   - With the token in hand, the BFF builds the request for the target API (e.g., `https://api.external.com/api/external-data`), adds the `Authorization: Bearer <token>` header, and executes it.
   - The response from the target API is then returned to the frontend.

## API Endpoint

**URL**: `/o/oauth-bff/proxy/{clientId}/{path:.*}`

- **HTTP Methods**: `GET`, `POST`, `PUT`, `DELETE`, etc. (the method is forwarded to the target API).
- **URL Parameters**:
  - `{clientId}`: The ID of the `OAuth Client` configured in the Control Panel. This ID is used to find the credentials and the token acquisition strategy.
  - `{path:.*}`: The full path of the target API to be called.

## Installation and Setup

### 1. Build and Deploy

To install the module, build it and deploy the generated artifact:

```bash
./gradlew jar
```

Then copy the JAR to your Liferay deploy folder:

```bash
cp build/libs/com.example.oauthbff.rest-1.0.0.jar $LIFERAY_HOME/deploy/
```

### 2. Configure an OAuth Client

1. After deployment, navigate to the Liferay **Control Panel**.
2. Go to the **OAuth BFF > Clients** section.
3. Click the add (+) button to create a new OAuth client.
4. Fill in the required fields:
   - **Alias**: A unique identifier used in the proxy URL (e.g., `billing-api`).
   - **Client ID**: The client ID provided by the identity provider.
   - **Client Secret**: The corresponding client secret.
   - **Token Endpoint URL**: The URL of the authorization server used to obtain the token.
   - **Type**: Select the strategy used to obtain the token (e.g., `Client Credentials` or `OIDC Session`).
   - **Scopes (Optional)**: Space-separated list of scopes required by the API.
   - **Allowed Endpoints**: A newline-separated list of relative path prefixes that this client is allowed to proxy (e.g., `me`, `users`, `invoices/`).
   - **Base URL**: The root URL of the external API this client proxies.
   - **Enabled**: Set to `true` to activate this client.

5. Save the form.

After saving, the BFF is ready to proxy calls to the configured API using the `alias` of the object you just created.

## ⚠️ Permissions and Access Control

> 🔐 **Important Note**: This project **does not yet support fine-grained access control based on Liferay Roles**. Any authenticated user can use the proxy endpoint (`/o/oauth-bff/proxy/...`) if the OAuthClient is correctly configured.
>
> Future versions may include support for specifying which roles can access specific OAuth Clients.