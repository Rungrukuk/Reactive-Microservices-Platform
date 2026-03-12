#!/bin/bash
set -e

CERTS_DIR="./certs"
JWT_DIR="./certs/jwt"

CA_PASSWORD=""
TRUSTSTORE_PASSWORD="truststorechangeit"
REDIS_KEYSTORE_PASSWORD="redischangeit"
AUTH_KEYSTORE_PASSWORD="authchangeit"
API_SERVER_KEYSTORE_PASSWORD="apiserverchangeit"
API_CLIENT_KEYSTORE_PASSWORD="apiclientchangeit"
USER_KEYSTORE_PASSWORD="userchangeit"
MONITORING_KEYSTORE_PASSWORD="monitoringchangeit"

echo "Creating directory structure..."
mkdir -p $CERTS_DIR/ca
mkdir -p $CERTS_DIR/redis
mkdir -p $CERTS_DIR/auth
mkdir -p $CERTS_DIR/user
mkdir -p $CERTS_DIR/api-gateway/server
mkdir -p $CERTS_DIR/api-gateway/client
mkdir -p $CERTS_DIR/monitoring
mkdir -p $CERTS_DIR/truststore
mkdir -p $JWT_DIR


# 1. CA Certificate
echo "Generating CA certificate..."
openssl genrsa -out $CERTS_DIR/ca/ca.key 4096
openssl req -new -x509 -days 3650 -key $CERTS_DIR/ca/ca.key \
    -out $CERTS_DIR/ca/ca.crt \
    -subj "/CN=microservicesPlatform-ca/O=microservicesPlatform/C=AZ"

# 2. Helper function to generate and sign certs
generate_cert() {
    local NAME=$1
    local DIR=$2
    local CN=$3

    echo "Generating certificate for $NAME..."
    openssl genrsa -out $DIR/$NAME.key 2048
    openssl req -new -key $DIR/$NAME.key \
        -out $DIR/$NAME.csr \
        -subj "/CN=$CN/O=microservicesPlatform/C=AZ"
    openssl x509 -req -days 3650 \
        -in $DIR/$NAME.csr \
        -CA $CERTS_DIR/ca/ca.crt \
        -CAkey $CERTS_DIR/ca/ca.key \
        -CAcreateserial \
        -extfile <(printf "subjectAltName=DNS:%s,DNS:localhost" "$CN") \
        -out $DIR/$NAME.crt
}

# 3. Service Certificates
generate_cert "redis"               $CERTS_DIR/redis              "redis"
generate_cert "auth-service"        $CERTS_DIR/auth               "auth-service"
generate_cert "user-service"        $CERTS_DIR/user               "user-service"
generate_cert "api-gateway"         $CERTS_DIR/api-gateway/server "api-gateway"
generate_cert "api-gateway-client"  $CERTS_DIR/api-gateway/client "api-gateway-client"
generate_cert "monitoring"          $CERTS_DIR/monitoring         "monitoring-service"


# 4. PKCS12 Keystores
echo "Creating PKCS12 keystores..."

openssl pkcs12 -export \
    -in $CERTS_DIR/redis/redis.crt \
    -inkey $CERTS_DIR/redis/redis.key \
    -out $CERTS_DIR/redis/redis.p12 \
    -name redis -passout pass:$REDIS_KEYSTORE_PASSWORD

openssl pkcs12 -export \
    -in $CERTS_DIR/auth/auth-service.crt \
    -inkey $CERTS_DIR/auth/auth-service.key \
    -out $CERTS_DIR/auth/auth-service.p12 \
    -name auth -passout pass:$AUTH_KEYSTORE_PASSWORD

openssl pkcs12 -export \
    -in $CERTS_DIR/user/user-service.crt \
    -inkey $CERTS_DIR/user/user-service.key \
    -out $CERTS_DIR/user/user-service.p12 \
    -name user-service -passout pass:$USER_KEYSTORE_PASSWORD

openssl pkcs12 -export \
    -in $CERTS_DIR/api-gateway/server/api-gateway.crt \
    -inkey $CERTS_DIR/api-gateway/server/api-gateway.key \
    -out $CERTS_DIR/api-gateway/server/api-gateway.p12 \
    -name api-gateway -passout pass:$API_SERVER_KEYSTORE_PASSWORD

openssl pkcs12 -export \
    -in $CERTS_DIR/api-gateway/client/api-gateway-client.crt \
    -inkey $CERTS_DIR/api-gateway/client/api-gateway-client.key \
    -out $CERTS_DIR/api-gateway/client/api-gateway-client.p12 \
    -name api-gateway-client -passout pass:$API_CLIENT_KEYSTORE_PASSWORD

openssl pkcs12 -export \
    -in $CERTS_DIR/monitoring/monitoring.crt \
    -inkey $CERTS_DIR/monitoring/monitoring.key \
    -out $CERTS_DIR/monitoring/monitoring.p12 \
    -name monitoring -passout pass:$MONITORING_KEYSTORE_PASSWORD


# 5. Truststore (contains CA cert)
echo "Creating truststore..."
keytool -import -trustcacerts -noprompt \
    -alias microservicesPlatform-ca \
    -file $CERTS_DIR/ca/ca.crt \
    -destkeystore $CERTS_DIR/truststore/truststore.p12 \
    -deststoretype PKCS12 \
    -storepass $TRUSTSTORE_PASSWORD


# 6. JWT Keys
echo "Generating JWT keys..."
for TYPE in access refresh service; do
    openssl genrsa -out $JWT_DIR/${TYPE}-private.pem 4096
    openssl rsa -in $JWT_DIR/${TYPE}-private.pem \
        -pubout -out $JWT_DIR/${TYPE}-public.pem
done


# 7. Write .env file
echo "Writing .env file..."
cat > .env << ENVEOF
# Database
DB_USERNAME=platform
DB_PASSWORD=change_this_password

# Redis
REDIS_USERNAME=auth_user
REDIS_PASSWORD=strongredispassword
REDIS_KEYSTORE_PASSWORD=${REDIS_KEYSTORE_PASSWORD}

# Truststore
TRUSTSTORE_PASSWORD=${TRUSTSTORE_PASSWORD}

# Auth Service
AUTH_KEYSTORE_PASSWORD=${AUTH_KEYSTORE_PASSWORD}

# API Gateway
API_KEYSTORE_PASSWORD=${API_SERVER_KEYSTORE_PASSWORD}
API_KEYSTORE_ALIAS=api-gateway

# API Gateway — client cert
API_CLIENT_KEYSTORE_PASSWORD=${API_CLIENT_KEYSTORE_PASSWORD}

# User Service
USER_KEYSTORE_PASSWORD=${USER_KEYSTORE_PASSWORD}

# Monitoring Service
MONITORING_KEYSTORE_PASSWORD=${MONITORING_KEYSTORE_PASSWORD}

# JWT Keys (auto-generated)
JWT_ACCESS_PRIVATE_KEY=$(openssl pkcs8 -topk8 -nocrypt -in $JWT_DIR/access-private.pem -outform DER | base64 -w 0)
JWT_ACCESS_PUBLIC_KEY=$(openssl rsa -pubin -in $JWT_DIR/access-public.pem -outform DER 2>/dev/null | base64 -w 0)
JWT_REFRESH_PRIVATE_KEY=$(openssl pkcs8 -topk8 -nocrypt -in $JWT_DIR/refresh-private.pem -outform DER | base64 -w 0)
JWT_REFRESH_PUBLIC_KEY=$(openssl rsa -pubin -in $JWT_DIR/refresh-public.pem -outform DER 2>/dev/null | base64 -w 0)
JWT_SERVICE_PRIVATE_KEY=$(openssl pkcs8 -topk8 -nocrypt -in $JWT_DIR/service-private.pem -outform DER | base64 -w 0)
JWT_SERVICE_PUBLIC_KEY=$(openssl rsa -pubin -in $JWT_DIR/service-public.pem -outform DER 2>/dev/null | base64 -w 0)
ENVEOF


# 8. Permissions
echo "Setting permissions..."
find $CERTS_DIR -name "*.key" -exec chmod 644 {} \;
find $CERTS_DIR -name "*.crt" -exec chmod 644 {} \;
find $CERTS_DIR -name "*.p12" -exec chmod 644 {} \;
find $JWT_DIR -name "*-private.pem" -exec chmod 644 {} \;
find $JWT_DIR -name "*-public.pem" -exec chmod 644 {} \;

echo ""
echo "Setup complete!"
echo "Certificates generated in ./certs/"
echo ".env file written"