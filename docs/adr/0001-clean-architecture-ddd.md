# ADR-0001: Adotar Clean Architecture combinada com DDD

- **Status**: Aceito
- **Data**: 2026-10-01
- **Participantes**: Grupo 117 (alinhado em reunião; Bernardo Camargo, Frederico Brion, Matheus, Vitor Melo, Caio Crevelaro)

## Contexto

O enunciado do Tech Challenge exige back-end monolítico com organização em camadas e aplicação
de DDD (event storming, agregados, linguagem ubíqua — já mapeados no Miro). O grupo avaliou
Clean Architecture e arquitetura hexagonal como opções de organização das camadas.

## Decisão

Adotar **Clean Architecture** combinada com **DDD**, com um pacote por bounded context
(`atendimento`, `catalogoestoque`, `identidadeacesso`) e camadas `domain` / `application` /
`infrastructure` dentro de cada contexto. Dependências apontam sempre para dentro: `domain` não
depende de framework; `application` define ports; `infrastructure` implementa (controllers,
adaptadores JPA).

## Consequências

- Positivas: regras de negócio testáveis sem Spring; fronteira clara entre contexts; facilita a
  relação entre documentação DDD e código; agentes de IA têm regras claras de onde colocar cada classe.
- Negativas: mais arquivos/indireção do que um MVC em camadas simples; custo aceito pelo escopo
  do projeto e pelo ganho didático.
- Alternativas descartadas: arquitetura hexagonal (equivalente em benefício, nomenclatura menos
  alinhada ao material da disciplina); MVC em camadas planas (borraria as fronteiras dos contexts).
