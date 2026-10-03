# Interiors 0.1.18 — revisão de exteriores e modelos

Publicação: 03/10/2026. **Native Aim permanece em 0.4.11 no pacote Steam.** Esta versão reúne o conteúdo concluído e verificado nesta rodada.

## O que entrou

- **Acabamentos de telhado:** 182 perfis físicos nativos de beirais, frisos e cantos, incluindo variantes de neve. Reutilizam o atlas do jogo e somam 7.284 triângulos no catálogo.
- **Águas do telhado:** completação das formas reconhecidas de duas e três faixas, com junções perpendiculares e recorte das interseções. Os casos verificados incluem alas da casa branca da região relatada. A correção não abrange automaticamente todo telhado do mapa.
- **Sinalização e cancelas:** a face escrita das placas STOP segue as quatro orientações nativas. As peças das cancelas conectadas compartilham altura e emendas consistentes.
- **Vasos e componentes elétricos:** quatro estados do vaso Ficus e 36 componentes elétricos com volume físico, posicionamento revisado e ligações de sprites.
- **Beliches e rádios:** quatro modelos revisados, com os materiais aprovados para esta entrega.
- **Assentos:** 79 peças de modelos — 20 cadeiras, 48 peças de sofás/poltronas e 11 modelos de restaurantes — com 307 ligações de sprites. Alguns defeitos de materiais desse lote ainda exigem revisão.
- **Correções mantidas:** posicionamento de janelas/vidros, objetos externos, compatibilidade Potato e Native Aim 0.4.11.
- **Apresentação:** duas prévias de móveis e uma montagem de mira aprovadas pelo autor; descrição e galeria reorganizadas, com avisos claros sobre o estado atual do projeto. A prévia de restaurantes com materiais problemáticos ficou fora desta nova seleção.

## Desempenho e verificação

O pacote registra **2.284 modelos, 2.335 ligações de sprites e 189.546 triângulos OBJ**. São totais do catálogo completo, não a quantidade de objetos desenhada simultaneamente. Usamos geometria simples e atlas compartilhados onde possível.

Os testes offline cobriram o importador real de modelos, 11 patches combinados em cinco classes, geometria e receitas de telhados/acabamentos, alinhamento das janelas, modo Potato, STOP/cancelas e componentes elétricos. [Resumo das verificações](Build-0.1.18-Validation.json).

Ainda falta a confirmação visual desta versão em uma nova sessão de gameplay e uma medição de FPS. A compatibilidade Potato foi verificada no runtime offline; não equivale a uma promessa de desempenho em qualquer computador.

## O que continua em revisão

**Muitos modelos já estão disponíveis no jogo, mas suas texturas ainda não estão prontas. Alguns modelos continuam com bugs visuais.** A disponibilidade de um modelo não significa que seus materiais estejam finalizados.

- Dutos e aparelhos HVAC flutuando, incluindo o caso em X10620/Y9450.
- A abertura de parede relatada, que ainda precisa ser conferida no jogo antes de alterar a geometria.
- Correspondência exata do telhado de quatro águas mostrado nas fotos e outros casos ainda não confirmados.
- Defeitos de texturas e materiais conhecidos nos assentos e demais modelos em revisão.
- Problemas de renderização e integração que envolvem o próprio Viewpoint.

Seguimos trabalhando nessas correções. A 0.1.18 não declara resolver todos os casos de telhado, parede ou textura do mapa. Packs de terceiros que substituem os mesmos sprites podem sobrepor resultados; a compatibilidade entre todos eles não está garantida.

**Reinicie completamente o jogo depois da atualização.**

[Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3811400314) · [Galeria aprovada](../Media/0.1.18/README.md)
