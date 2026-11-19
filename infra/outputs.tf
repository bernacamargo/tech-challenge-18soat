output "aws_region" {
  description = "Região AWS usada pelo stack."
  value       = var.aws_region
}

output "vpc_id" {
  description = "ID da VPC."
  value       = aws_vpc.this.id
}

output "eks_cluster_name" {
  description = "Nome do cluster EKS (usar em aws eks update-kubeconfig)."
  value       = aws_eks_cluster.this.name
}

output "eks_cluster_endpoint" {
  description = "Endpoint da API do Kubernetes."
  value       = aws_eks_cluster.this.endpoint
}

output "eks_cluster_arn" {
  description = "ARN do cluster EKS."
  value       = aws_eks_cluster.this.arn
}

output "eks_cluster_security_group_id" {
  description = "Security group gerenciado do cluster (nodes)."
  value       = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

output "rds_endpoint" {
  description = "Endpoint do RDS PostgreSQL (host:porta)."
  value       = aws_db_instance.this.endpoint
}

output "rds_address" {
  description = "Hostname do RDS (sem porta)."
  value       = aws_db_instance.this.address
}

output "rds_port" {
  description = "Porta do RDS."
  value       = aws_db_instance.this.port
}

output "rds_db_name" {
  description = "Nome do banco criado no RDS."
  value       = aws_db_instance.this.db_name
}

output "jdbc_url_template" {
  description = "Template JDBC para a aplicação (preencher usuário/senha via Secret do k8s)."
  value       = "jdbc:postgresql://${aws_db_instance.this.address}:${aws_db_instance.this.port}/${aws_db_instance.this.db_name}"
}
