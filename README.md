# Project ViewPoint — 3D Interiors & Native Aim

Projeto de colaboração do mod **Project ViewPoint: 3D Interiors & Native Aim [B42.21]**, mantido por Hiroki (conta GitHub `Hirouou`). Inclui código de interiores, sistema de mira nativa e uma biblioteca de 603 modelos originais com mapas de textura e guias UV.

## Código do projeto

- [Sistema de mira nativa](Scripts/NativeAim/README.md): fontes Java, controle Lua e calibrações para propor ajustes de alinhamento, movimento, respiração e coice.
- [Integração dos interiores](Scripts/Interiors/README.md): fontes Java e controle Lua para os modelos e estados dos objetos.
- [Preparação e compilação](Docs/DEVELOPMENT.md): dependências externas e compilação dos dois módulos, sem caminhos pessoais ou instalação automática.

Este repositório é uma base de desenvolvimento e revisão, não uma cópia da instalação do jogo nem um pacote pronto para substituir a versão da Steam. A biblioteca UV ainda precisa de integração e revisão em jogo. Arquivos do Project Zomboid, Viewpoint e ZombieBuddy não são redistribuídos aqui.

## Encontrar e pintar um modelo

Abra [a galeria pesquisável de UVs](Docs/UV-GALLERY.html) e busque pelo ID. Cada cartão abre o guia visual daquele modelo. O arquivo de pintura correspondente fica em `textures/PNG/<id>_UV.png`; o modelo fica em `models/<id>.obj` e seu MTL em `Materials/<id>.mtl`.

Para pintar, mantenha o nome e a resolução do PNG. Cada mapa já começa com a textura que o modelo usa no mod, remapeada para as UVs organizadas, e pode ser editado no Aseprite. O guia `textures/UV-Guides/<id>_UV-Guia.png` identifica os contornos e as ilhas. Os dois rádios `pz_house_makeshift_ham` e `pz_house_makeshift_radio` foram reservados para retrabalho e não fazem parte desta revisão.

## Ver no Blender

Abra `models/ProjectViewPoint-UV-Library-Surfaces.blend`. A biblioteca contém os 603 modelos organizados em coleções por lote. Para atualizar o modelo após salvar um PNG, execute `Scripts/Reload-UV-PNGs.py` uma vez pelo editor de texto do Blender; as imagens são recarregadas automaticamente.

O índice em [Docs/UV-INDEX.csv](Docs/UV-INDEX.csv) facilita filtrar por nome. [Docs/UV-MANIFEST.json](Docs/UV-MANIFEST.json) registra resoluções e hashes. [Docs/UV-WORKFLOW.md](Docs/UV-WORKFLOW.md) explica o fluxo completo.

## Arquivos e direitos

A biblioteca contém 603 OBJ, 603 MTL, 603 mapas PNG, 603 guias UV e uma cena Blender editável. Hiroki confirmou a autoria original dos modelos e mapas e autorizou sua publicação para colaboração no Project ViewPoint. Projetos Aseprite/Zprite, backups e folhas-fonte do jogo não fazem parte deste pacote.

**© 2026 Hiroki. Todos os direitos reservados / All Rights Reserved.** A [licença](LICENSE.md) permite consultar, baixar, criar forks e editar os arquivos para propor melhorias ao mod por Pull Request. Reutilização em outro mod, jogo, projeto ou produto, venda e redistribuição independente exigem autorização prévia por escrito. Contribuições seguem o [guia de contribuição](CONTRIBUTING.md).

O pacote serve para revisão e colaboração. A integração no mod e qualquer atualização do Steam Workshop dependem da aprovação do mantenedor.

## Como enviar melhorias sem alterar a biblioteca oficial

Crie um **fork** (uma cópia na sua própria conta), faça as alterações em uma branch dessa cópia e abra um **Pull Request** para este repositório. Sua proposta aparece na aba **Pull requests**, separada dos arquivos oficiais, para Hiroki revisar, pedir ajustes, aceitar ou recusar.

Abrir um Pull Request não altera a branch `main`. Somente Hiroki aprova e incorpora as mudanças; colaboradores da comunidade não recebem acesso direto de escrita ao repositório oficial. Até a aprovação e incorporação, quem baixar a biblioteca oficial continuará recebendo a versão do mantenedor.

## Comunidade

Converse sobre contribuições e acompanhe o projeto no [Discord do Project ViewPoint](https://discord.gg/ME53neUunA).
