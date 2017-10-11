#!/usr/bin/env bash
set -o errexit
set -o errtrace

source .aws/scripts/init-awscli.sh
source .aws/scripts/init-ansible-vault.sh

DB_INSTANCE_ID="camunda-optimize-db-stage"
DB_NAME=$(awscli rds describe-db-instances --query "DBInstances[?DBInstanceIdentifier=='${DB_INSTANCE_ID}'].DBName" --output text)
DB_HOST=$(awscli rds describe-db-instances --query "DBInstances[?DBInstanceIdentifier=='${DB_INSTANCE_ID}'].Endpoint.Address" --output text)
REMOTE_HOST_IP=$(awscli ec2 describe-instances --filters 'Name=tag:Name,Values=Camunda Optimize*' --query 'Reservations[*].Instances[*].PublicIpAddress' --output text)
CAMUNDA_OPTIMIZE_VERSION=$(grep 'SNAPSHOT' pom.xml | sed 's/.*<version>\(.*\)<\/version>/\1/')

cd .aws/ansible || exit 1

cat << EOF > hosts
[optimize]
${REMOTE_HOST_IP}
EOF

ansible-playbook bootstrap-python.yml -i hosts

ansible-playbook camunda.yml \
  -i hosts \
  -e db_name=${DB_NAME} \
  -e db_host=${DB_HOST} \
  -e camunda_optimize_version="${CAMUNDA_OPTIMIZE_VERSION}"
