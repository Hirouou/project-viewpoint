# Native Aim — código para colaboração

Fontes do módulo de mira nativa 0.4.9 do Project ViewPoint, mantido por Hiroki. As dependências do jogo, Viewpoint e ZombieBuddy não fazem parte deste repositório.

- `source/local/vpads/`: alinhamento da mira/câmera, esqueleto, cinemática dos braços, prioridade de ações, captura, respiração, movimento e coice.
- `mod/42/media/lua/client/ViewpointADSTest.lua`: ativação e comunicação com o módulo Java.
- `mod/42/media/aim-calibration/`: parâmetros e matrizes numéricas de calibração, sem meshes ou imagens das armas do jogo.
- `mod/common/mod.info`: identificação e dependências do módulo.

Leia [DEVELOPMENT.md](../../Docs/DEVELOPMENT.md) para compilar. Proponha ajustes numa branch de seu fork e abra um Pull Request. Informe as armas e situações verificadas, a taxa de quadros e o efeito em primeira/terceira pessoa. Não publique saves, logs pessoais ou arquivos extraídos do jogo.

A cópia pública omite a limpeza de identificadores antigos de animação que continham um nome pessoal. Ela não é um instalador nem uma ferramenta de migração de versões antigas. Alterações aceitas ainda dependem da revisão e integração do mantenedor.

All Rights Reserved — Hiroki. Uso e alterações exclusivamente para contribuir com Project ViewPoint, conforme [LICENSE.md](../../LICENSE.md).
