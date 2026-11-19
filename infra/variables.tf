variable "aws_region" {
  description = "Região AWS onde o cluster e o banco serão provisionados."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Nome curto do ambiente (tags e prefixos de recurso)."
  type        = string
  default     = "fase2"
}

variable "project_name" {
  description = "Prefixo dos recursos (VPC, EKS, RDS)."
  type        = string
  default     = "oficina"
}

variable "vpc_cidr" {
  description = "CIDR da VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "eks_cluster_version" {
  description = "Versão do Kubernetes no EKS."
  type        = string
  default     = "1.31"
}

variable "eks_node_instance_types" {
  description = "Tipos de instância do node group gerenciado."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "eks_node_desired_size" {
  description = "Quantidade desejada de nodes."
  type        = number
  default     = 2
}

variable "eks_node_min_size" {
  description = "Quantidade mínima de nodes."
  type        = number
  default     = 1
}

variable "eks_node_max_size" {
  description = "Quantidade máxima de nodes."
  type        = number
  default     = 3
}

variable "rds_instance_class" {
  description = "Classe da instância RDS PostgreSQL."
  type        = string
  default     = "db.t4g.micro"
}

variable "rds_allocated_storage" {
  description = "Armazenamento inicial do RDS (GiB)."
  type        = number
  default     = 20
}

variable "rds_db_name" {
  description = "Nome do banco PostgreSQL (schema da app é criado pelo Flyway)."
  type        = string
  default     = "oficina"
}

variable "rds_username" {
  description = "Usuário master do RDS (troque em produção; não commit secrets)."
  type        = string
  default     = "oficina_admin"
  sensitive   = true
}

variable "rds_password" {
  description = "Senha master do RDS. Obrigatória no apply; use TF_VAR_rds_password ou tfvars local não versionado."
  type        = string
  sensitive   = true
}
