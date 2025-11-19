#!/bin/bash
# =================================================================
# Script de inicialización de bases de datos PostgreSQL
# Crea múltiples bases de datos y usuarios para cada microservicio
# =================================================================

set -e
set -u

# Función para crear base de datos y usuario
create_database_and_user() {
    local database=$1
    local user=$2
    local password=$3

    echo "Creating database '$database' and user '$user'..."

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        -- Crear usuario si no existe
        DO \$\$
        BEGIN
            IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$user') THEN
                CREATE USER $user WITH PASSWORD '$password';
            END IF;
        END
        \$\$;

        -- Crear base de datos si no existe
        SELECT 'CREATE DATABASE $database OWNER $user'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$database')\gexec

        -- Otorgar todos los privilegios
        GRANT ALL PRIVILEGES ON DATABASE $database TO $user;

        -- Conectar a la base de datos y otorgar privilegios en el schema public
        \c $database
        GRANT ALL ON SCHEMA public TO $user;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $user;
EOSQL

    echo "Database '$database' and user '$user' created successfully!"
}

# Función principal
main() {
    echo "============================================="
    echo "Initializing Invoices Backend Databases"
    echo "============================================="

    # Leer la variable POSTGRES_MULTIPLE_DATABASES y crear cada base de datos
    if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
        IFS=',' read -ra DATABASES <<< "$POSTGRES_MULTIPLE_DATABASES"

        for db in "${DATABASES[@]}"; do
            # Eliminar espacios en blanco
            db=$(echo "$db" | xargs)

            # Definir usuario y password basado en el nombre de la BD
            case "$db" in
                userdb)
                    user="user_service_user"
                    password="${USER_DB_PASSWORD:-user_pass_2025}"
                    ;;
                invoicedb)
                    user="invoice_service_user"
                    password="${INVOICE_DB_PASSWORD:-invoice_pass_2025}"
                    ;;
                documentdb)
                    user="document_service_user"
                    password="${DOCUMENT_DB_PASSWORD:-document_pass_2025}"
                    ;;
                tracedb)
                    user="trace_service_user"
                    password="${TRACE_DB_PASSWORD:-trace_pass_2025}"
                    ;;
                *)
                    user="${db}_user"
                    password="${db}_pass"
                    ;;
            esac

            create_database_and_user "$db" "$user" "$password"
        done
    fi

    echo "============================================="
    echo "Database initialization completed!"
    echo "============================================="
}

# Ejecutar función principal
main
