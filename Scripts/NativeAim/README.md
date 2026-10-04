# Native Aim — código para colaboração

O Native Aim é distribuído como um [mod separado no Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3813269725), mantido por Hiro.uou. A versão publicada é **0.4.11**. Ele pode ser usado sem o pacote 3D Models e mantém o ID de mod **`ViewpointADSTest`**; Viewpoint e ZombieBuddy continuam sendo dependências.

Os arquivos desta pasta são um **snapshot de colaboração da 0.4.9**. A separação dos itens na Steam não atualiza esses fontes para reproduzir o binário 0.4.11. Compilar esta pasta gera o snapshot descrito aqui; para jogar com a versão publicada, use o item da Steam. As dependências do jogo, Viewpoint e ZombieBuddy não fazem parte deste repositório.

Quem já usava a mira no pacote completo deve assinar o novo item e ativar `ViewpointADSTest`. Consulte [a migração dos dois mods](../../Docs/MOD-SPLIT.md).

- `source/local/vpads/`: alinhamento da mira/câmera, esqueleto, cinemática dos braços, prioridade de ações, captura, respiração, movimento e coice.
- `mod/42/media/lua/client/ViewpointADSTest.lua`: ativação e comunicação com o módulo Java.
- `mod/42/media/aim-calibration/`: parâmetros e matrizes numéricas de calibração, sem meshes ou imagens das armas do jogo.
- `mod/common/mod.info`: identificação e dependências do módulo.

Leia [DEVELOPMENT.md](../../Docs/DEVELOPMENT.md) para compilar. Proponha ajustes numa branch de seu fork e abra um Pull Request. Informe as armas e situações verificadas, a taxa de quadros e o efeito em primeira/terceira pessoa. Não publique saves, logs pessoais ou arquivos extraídos do jogo.

A cópia pública omite a limpeza de identificadores antigos de animação que continham um nome pessoal. Ela não é um instalador nem uma ferramenta de migração de versões antigas. Alterações aceitas ainda dependem da revisão e integração do mantenedor.

All Rights Reserved — Hiro.uou. Uso e alterações exclusivamente para contribuir com Project ViewPoint, conforme [LICENSE.md](../../LICENSE.md).
