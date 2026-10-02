# Interiores — código para colaboração

Fontes Java e Lua do módulo de interiores 0.1.13 do Project ViewPoint, mantido por Hiroki.

- `source/local/vpinteriors/`: seleção dos estados visuais, integração de pias/balcões e carregamento do módulo.
- `mod/42/media/lua/client/ViewpointFurnitureFix.lua`: registro e controle da integração com o Viewpoint.
- `mod/42/media/modelpacks/furniturefix/pack.properties`: associações de sprites/modelos do snapshot de runtime. Os nomes do jogo são referências de integração, não arquivos redistribuídos.
- `mod/common/mod.info`: identificação e dependências.

A biblioteca editável em `models/`, `Materials/` e `textures/` possui UVs preparadas para colaboração. Não substitua automaticamente o pack instalado: o manifesto do runtime e os caminhos de textura precisam de integração e revisão em jogo. As builds locais não incluem os assets de runtime nem o poster.

Leia [DEVELOPMENT.md](../../Docs/DEVELOPMENT.md). Envie mudanças em uma branch de seu fork e abra um Pull Request para Hiroki revisar. Mantenha inventário, interação e colisão sob responsabilidade do jogo.

All Rights Reserved — Hiroki. Uso exclusivamente para contribuir com Project ViewPoint, conforme [LICENSE.md](../../LICENSE.md).
