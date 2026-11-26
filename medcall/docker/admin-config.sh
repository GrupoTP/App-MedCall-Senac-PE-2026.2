#!/bin/sh
# Escreve as configurações dos portais (/admin/config.js) a partir do ambiente do container.
# O nginx roda os scripts de /docker-entrypoint.d/ antes de subir.
#
#   ADMIN_API_BASE_URL  endereço da API, como https://api.exemplo.com. Sem ela, o arquivo
#                       empacotado fica como está (API na mesma origem dos portais).
#
# Se o arquivo vier montado (ConfigMap, somente leitura), ele vale e este script não mexe nele.
set -eu

target=/usr/share/nginx/html/admin/config.js
value="${ADMIN_API_BASE_URL:-}"

if [ -z "$value" ]; then
    exit 0
fi
if ! printf '%s' "$value" | grep -Eq '^https?://[A-Za-z0-9.:/_-]+$'; then
    echo "admin-config: ADMIN_API_BASE_URL inválida: $value" >&2
    exit 1
fi
if [ ! -w "$target" ]; then
    echo "admin-config: $target é somente leitura; usando o arquivo montado."
    exit 0
fi

cat > "$target" <<EOF
// Gerado na subida do container a partir de ADMIN_API_BASE_URL (docker/admin-config.sh).
window.MEDCALL_ADMIN_CONFIG = {
    apiBaseUrl: "$value",
};
EOF
echo "admin-config: apiBaseUrl=$value"
