# Desenvolvimento do Project ViewPoint

Este repositório inclui o snapshot de colaboração do Native Aim 0.4.9 e os fontes de Interiores 0.1.19, além dos modelos editáveis e mapas UV. A versão jogável do Native Aim na Steam é 0.4.11; os fontes de colaboração dessa pasta não representam integralmente aquele binário. Os dois módulos agora têm [itens separados no Workshop](MOD-SPLIT.md). O repositório não inclui o código de terceiros que fornece o renderizador Viewpoint, o agente ZombieBuddy ou o próprio jogo.

## Dependências locais

- Python 3 para executar a ferramenta de compilação.
- JDK com suporte à compilação Java 17 e leitura das classes da versão instalada do jogo. Se essas classes exigirem uma versão mais nova que seu JDK, use Eclipse ECJ compatível com a versão do jogo.
- Instalação legítima do Project Zomboid 42.21.
- Viewpoint compatível com PZ42.21 e ZombieBuddy base2.3.4+. ZombieBuddy Extensions é opcional.

Obtenha e instale essas dependências pelos canais dos respectivos autores. Não copie seus binários ou fontes para um Pull Request. Forneça os caminhos da sua própria instalação: o projeto não grava caminhos pessoais de outro computador.

## Compilar o código

Na raiz do repositório, execute:

```text
python Scripts/build_project.py --module all --game-dir "<pasta do jogo>" --jdk-dir "<pasta do JDK>" --dependency-jar "<Viewpoint.jar>" --dependency-jar "<ZombieBuddy.jar>"
```

Se bibliotecas adicionais não estiverem nos JARs da pasta do jogo, informe cada uma com outro `--dependency-jar`. Use `--module NativeAim` ou `--module Interiors` para trabalhar em apenas um módulo.

Os resultados ficam em `Builds/` e são ignorados pelo Git. A ferramenta não copia, remove ou altera nada na instalação do jogo. Ela compila apenas o código original e copia os controles/configurações do módulo. O módulo Interiors inclui os assets aprovados da pasta mod. A biblioteca UV de colaboração continua separada e exige revisão antes da integração.

### Classes recentes do jogo / Eclipse ECJ

Se `javac` informar que as classes do jogo têm uma versão mais recente que a suportada, use um JDK compatível ou acrescente `--compiler-jar "<ecj.jar>"` ao comando. O fluxo verificado usa Eclipse ECJ 4.38 e o Java incluído em `jre64/bin` do jogo para compilar os fontes como Java 17; o JDK fornece a ferramenta `jar`. Para outro runtime, informe `--java-bin "<executável java>"`. O compilador ECJ e o runtime não são redistribuídos no repositório.

Feche o jogo antes de qualquer instalação manual e preserve seus backups. Uma compilação bem-sucedida não comprova alinhamento visual ou compatibilidade em partida. Descreva as verificações efetuadas no Pull Request; não envie saves, dados pessoais, logs locais completos ou credenciais.

## Revisão e direitos

Faça as mudanças no seu fork e abra um Pull Request para a branch `main` do repositório oficial. Só Hiro.uou decide se aceita e incorpora a proposta. O processo não concede acesso direto de escrita, nem permite reutilizar o código ou os modelos em outros projetos. Consulte [CONTRIBUTING.md](../CONTRIBUTING.md) e [LICENSE.md](../LICENSE.md).
