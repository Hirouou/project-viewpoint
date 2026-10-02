# Modelos e arquivos de UV

Esta pasta contém 603 modelos OBJ e a biblioteca Blender `ProjectViewPoint-UV-Library-Surfaces.blend`. Cada OBJ tem um material MTL homônimo em `../Materials`. Os MTLs apontam para um PNG homônimo em `../textures/PNG`.

As UVs foram organizadas por superfícies conectadas: triângulos coplanares formam uma ilha, e costuras em superfícies curvas limitam a distorção. O guia correspondente fica em `../textures/UV-Guides`. Consulte `../Docs/UV-GALLERY.html` para buscar por nome ou `../Docs/UV-INDEX.csv` para a lista em formato tabular.

Para atualizar a imagem no Blender, abra a biblioteca, execute `../Scripts/Reload-UV-PNGs.py` uma vez no editor de texto e salve o PNG em `../textures/PNG`. A cena mostra os 603 modelos. Dois rádios reservados para retrabalho foram excluídos.

Cada PNG começa com a textura do próprio modelo remapeada para a nova UV; edite o arquivo no Aseprite mantendo seu nome e tamanho. O pacote contém os mapas por modelo, sem incluir as folhas-fonte completas. Consulte o README raiz e os arquivos de licença antes de propor alterações.
