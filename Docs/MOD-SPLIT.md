# Project ViewPoint — dois mods no Workshop

O Project ViewPoint passa a oferecer os modelos 3D e o sistema de mira em itens separados. Você pode escolher uma das partes ou usar as duas juntas. Ambos continuam precisando de **Project Zomboid 42.21**, **Viewpoint** e **ZombieBuddy**, com as dependências compatíveis indicadas nas páginas da Steam.

| Mod | Item no Workshop | ID para ativar no jogo | Versão |
| --- | --- | --- | --- |
| Project ViewPoint: 3D Models | [Item existente](https://steamcommunity.com/sharedfiles/filedetails/?id=3811400314) | `ViewpointFurnitureFix` | 0.1.18 |
| Project ViewPoint: Native Aim | [Novo item](https://steamcommunity.com/sharedfiles/filedetails/?id=3813269725) | `ViewpointADSTest` | 0.4.11 |

O **3D Models** inclui os modelos de interiores e exteriores e sua integração. O **Native Aim** inclui o alinhamento da mira em primeira pessoa, os ajustes de movimento e respiração e o coice. O Native Aim mantém as ações nativas do personagem e a comparação de alinhamento com **F7**.

## Para quem já usava o pacote completo

1. Mantenha a assinatura do item existente se quiser continuar usando os modelos 3D.
2. Assine o novo item **Native Aim** para continuar usando a mira em primeira pessoa.
3. Com o jogo fechado, deixe a Steam concluir os downloads.
4. No menu de mods e na configuração da partida ou servidor, ative `ViewpointFurnitureFix` para os modelos e/ou `ViewpointADSTest` para a mira, junto das dependências.

Os IDs de mod foram preservados. O ID de Workshop da mira é novo e precisa ser adicionado às configurações que enumeram itens do Workshop, como as de um servidor. Evite instalar duas cópias do mesmo `ViewpointADSTest`, por exemplo uma cópia local antiga e o novo download da Steam.

## Conteúdo e limitações

A separação mantém **3D Models 0.1.18** e **Native Aim 0.4.11**. A revisão local de Muldraugh e os experimentos posteriores com janelas e aparelhos HVAC não fazem parte desta publicação. Consulte [as notas da 0.1.18](PATCH-0.1.18.md) para o conteúdo verificado dessa versão.

Muitos modelos já estão no jogo, mas ainda não têm suas texturas prontas. Alguns modelos apresentam defeitos visuais, e as correções de renderização que envolvem o próprio Viewpoint continuam em andamento. A separação dos downloads não encerra esse trabalho.

## Colaboração no GitHub

O repositório continua reunindo [os modelos e fontes de Interiors](../Scripts/Interiors/README.md) e [o snapshot de fontes do Native Aim](../Scripts/NativeAim/README.md). Os fontes públicos de Native Aim ainda correspondem ao snapshot 0.4.9 de colaboração; a versão jogável distribuída na Steam é 0.4.11.

Para apoiar o desenvolvimento, visite o [Ko-fi do estúdio](https://ko-fi.com/chocomilkestudio).
