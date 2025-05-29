# Keycloak Prefixed Attribute Mapper Extension

A custom Keycloak OIDC Identity Provider Mapper that allows you to import claims from an external OIDC identity provider and prefix the values before storing them as user attributes.

The extension is compatible with latest versions of Keycloak (tested on versions 26+).

## Build

### Build Requirements

- Java 17+
- Maven 3.6+

Clone the repository and build the JAR using Maven:

```sh
mvn clean package
```

The built JAR will be located at:

```
target/prefixed-attribute-mapper-extension-1.0.0.jar
```

## Install

1. **Copy the JAR**

   Download the prebuilt JAR from the `releases` section or build it using the above instructions, then copy the JAR file to your Keycloak server's `providers` directory:

   ```
   cp target/prefixed-attribute-mapper-extension-1.0.0.jar /opt/keycloak/providers/
   ```

2. **Restart Keycloak**

   Restart your Keycloak server to load the new provider:

   ```sh
   ./bin/kc.sh restart
   ```

3. **Configure in Keycloak Admin Console**
   - Go to **Identity Providers** and select your OIDC provider.
   - Go to the **Mappers** tab and click **Add Mapper**.
   - Select **Prefixed Attribute Importer** as the mapper type.
   - Configure the claim, user attribute, and prefix as needed.

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Author

[pszabo891](https://github.com/pszabo891)