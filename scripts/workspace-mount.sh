#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(dirname -- "$script_dir")

check_only=false
refresh=false
case ${1:-} in
    '') ;;
    --check) check_only=true ;;
    --refresh) refresh=true ;;
    *)
        printf 'Usage : %s [--check|--refresh]\n' "$0" >&2
        exit 2
        ;;
esac

# Les variables du processus ont priorité sur les fichiers sous env/.
process_workspace_set=${KB_GENAI_BUILDER_WORKSPACE_DIR+yes}
process_workspace_value=${KB_GENAI_BUILDER_WORKSPACE_DIR-}
process_remote_set=${KB_GENAI_BUILDER_RCLONE_REMOTE+yes}
process_remote_value=${KB_GENAI_BUILDER_RCLONE_REMOTE-}
process_read_only_set=${KB_GENAI_BUILDER_RCLONE_READ_ONLY+yes}
process_read_only_value=${KB_GENAI_BUILDER_RCLONE_READ_ONLY-}
process_bin_set=${KB_GENAI_BUILDER_RCLONE_BIN+yes}
process_bin_value=${KB_GENAI_BUILDER_RCLONE_BIN-}
process_log_dir_set=${KB_GENAI_BUILDER_RCLONE_LOG_DIR+yes}
process_log_dir_value=${KB_GENAI_BUILDER_RCLONE_LOG_DIR-}
process_rclone_config_pass_set=${RCLONE_CONFIG_PASS+yes}
process_rclone_config_pass_value=${RCLONE_CONFIG_PASS-}

set -a
. "$project_dir/env/default.env"
if [ -f "$project_dir/env/.env.local" ]; then
    . "$project_dir/env/.env.local"
fi
if [ -f "$project_dir/env/.env.user.local" ]; then
    . "$project_dir/env/.env.user.local"
fi
set +a

if [ "$process_workspace_set" = yes ]; then
    KB_GENAI_BUILDER_WORKSPACE_DIR=$process_workspace_value
fi
if [ "$process_remote_set" = yes ]; then
    KB_GENAI_BUILDER_RCLONE_REMOTE=$process_remote_value
fi
if [ "$process_read_only_set" = yes ]; then
    KB_GENAI_BUILDER_RCLONE_READ_ONLY=$process_read_only_value
fi
if [ "$process_bin_set" = yes ]; then
    KB_GENAI_BUILDER_RCLONE_BIN=$process_bin_value
fi
if [ "$process_log_dir_set" = yes ]; then
    KB_GENAI_BUILDER_RCLONE_LOG_DIR=$process_log_dir_value
fi
if [ "$process_rclone_config_pass_set" = yes ]; then
    RCLONE_CONFIG_PASS=$process_rclone_config_pass_value
fi

workspace_dir=${KB_GENAI_BUILDER_WORKSPACE_DIR:?KB_GENAI_BUILDER_WORKSPACE_DIR est requise}
remote=${KB_GENAI_BUILDER_RCLONE_REMOTE:?KB_GENAI_BUILDER_RCLONE_REMOTE est requise}
read_only=${KB_GENAI_BUILDER_RCLONE_READ_ONLY:?KB_GENAI_BUILDER_RCLONE_READ_ONLY est requise}

case $workspace_dir in
    /*) ;;
    *) workspace_dir=$project_dir/$workspace_dir ;;
esac

case $read_only in
    true|false) ;;
    *)
        printf 'ERROR: KB_GENAI_BUILDER_RCLONE_READ_ONLY doit valoir true ou false.\n' >&2
        exit 1
        ;;
esac

if [ -n "${KB_GENAI_BUILDER_RCLONE_LOG_DIR:-}" ]; then
    log_dir=$KB_GENAI_BUILDER_RCLONE_LOG_DIR
elif [ -n "${XDG_CACHE_HOME:-}" ]; then
    log_dir=$XDG_CACHE_HOME/rclone
else
    log_dir=${HOME:?HOME doit être défini}/.cache/rclone
fi

if [ -n "${KB_GENAI_BUILDER_RCLONE_BIN:-}" ]; then
    rclone_bin=$KB_GENAI_BUILDER_RCLONE_BIN
elif command -v rclone >/dev/null 2>&1; then
    rclone_bin=$(command -v rclone)
elif [ -x "${HOME:?HOME doit être défini}/.local/bin/rclone" ]; then
    rclone_bin=$HOME/.local/bin/rclone
else
    printf '%s\n' 'ERROR: rclone est introuvable; installez-le ou définissez KB_GENAI_BUILDER_RCLONE_BIN.' >&2
    exit 1
fi

if [ ! -x "$rclone_bin" ]; then
    printf 'ERROR: rclone n’est pas exécutable : %s\n' "$rclone_bin" >&2
    exit 1
fi

if ! "$rclone_bin" lsf "$remote" --max-depth 1 >/dev/null; then
    printf 'ERROR: le remote rclone est inaccessible : %s\n' "$remote" >&2
    exit 1
fi

if [ "$check_only" = true ]; then
    printf 'Configuration valide :\n'
    printf '  remote       : %s\n' "$remote"
    printf '  workspace    : %s\n' "$workspace_dir"
    printf '  lecture seule: %s\n' "$read_only"
    printf '  rclone       : %s\n' "$rclone_bin"
    printf '  journaux     : %s\n' "$log_dir"
    exit 0
fi

mounted_fs=$(findmnt -rn -M "$workspace_dir" -o FSTYPE 2>/dev/null || true)
if [ "$mounted_fs" = "fuse.rclone" ]; then
    if [ "$refresh" = true ]; then
        printf 'Rafraîchissement du montage OneDrive : démontage de %s\n' "$workspace_dir"
        fusermount -uz "$workspace_dir"
    elif find "$workspace_dir" -maxdepth 0 -print >/dev/null 2>&1; then
        printf 'Workspace déjà monté : %s\n' "$workspace_dir"
        exit 0
    else
        printf 'WARN: nettoyage d’un ancien montage rclone inaccessible : %s\n' "$workspace_dir" >&2
        fusermount -uz "$workspace_dir"
    fi
elif [ -n "$mounted_fs" ]; then
    printf 'ERROR: le point est déjà utilisé par un système de fichiers %s : %s\n' "$mounted_fs" "$workspace_dir" >&2
    exit 1
fi

mkdir -p "$workspace_dir" "$log_dir"

if [ -n "$(find "$workspace_dir" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
    printf 'ERROR: le point de montage n’est pas vide : %s\n' "$workspace_dir" >&2
    exit 1
fi

PATH=$(dirname -- "$rclone_bin"):$PATH
export PATH

set -- "$remote" "$workspace_dir" \
    --dir-cache-time 5m \
    --poll-interval 1m \
    --daemon \
    --log-file "$log_dir/kb-genai-builder-sharepoint.log" \
    --log-level INFO
if [ "$read_only" = true ]; then
    set -- "$@" --read-only
fi

"$rclone_bin" mount "$@"

attempt=0
while [ "$attempt" -lt 5 ]; do
    if mountpoint -q "$workspace_dir"; then
        printf 'Workspace monté (lecture seule=%s) : %s -> %s\n' "$read_only" "$remote" "$workspace_dir"
        exit 0
    fi
    attempt=$((attempt + 1))
    sleep 1
done

printf 'ERROR: le montage n’est pas actif : %s\n' "$workspace_dir" >&2
exit 1
