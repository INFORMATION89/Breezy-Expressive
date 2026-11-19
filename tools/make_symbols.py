#!/usr/bin/env python3
"""Regenerates ui/expressive/components/SolsticeIcons.kt from tools/symbols.txt.

Downloads each Material Symbol (Rounded, weight 400, grade 0) at optical sizes 24 and 20 from
google/material-design-icons (Apache-2.0) and writes the Kotlin catalog. Run from the repo root:
    python3 tools/make_symbols.py
"""
import pathlib
import re
import urllib.request

R = pathlib.Path(__file__).resolve().parent.parent
F = R / "app/src/main/kotlin/org/breezyweather/ui/expressive/components/SolsticeIcons.kt"
BASE = "https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web"
_cache = {}


def path(size, n):
    """The SVG path of symbol n (n may end in _fill) at optical size 24 ('svg') or 20 ('svg20')."""
    name = n[:-5] if n.endswith("_fill") else n
    fill = "_fill1" if n.endswith("_fill") else ""
    px = "20" if size == "svg20" else "24"
    url = f"{BASE}/{name}/materialsymbolsrounded/{name}{fill}_{px}px.svg"
    if url not in _cache:
        with urllib.request.urlopen(url) as r:
            svg = r.read().decode()
        ps = re.findall(r'<path d="([^"]+)"', svg)
        assert len(ps) == 1, url
        _cache[url] = ps[0]
    return _cache[url]


def pascal(n):
    return "".join(p.capitalize() for p in n.split("_"))


entries_txt = [
    l.split() for l in (R / "tools/symbols.txt").read_text().splitlines() if l.strip() and not l.startswith("#")
]
syms = [e[0] for e in entries_txt]
fills = [e[0] for e in entries_txt if "fill" in e[1:]]
mirror = {e[0] for e in entries_txt if "mirror" in e[1:]}
header = F.read_text().split('package ')[0]
out = [header.rstrip()+'\n', 'package org.breezyweather.ui.expressive.components\n',
'import androidx.compose.ui.graphics.Color',
'import androidx.compose.ui.graphics.SolidColor',
'import androidx.compose.ui.graphics.vector.ImageVector',
'import androidx.compose.ui.graphics.vector.PathParser',
'import androidx.compose.ui.graphics.vector.group',
'import androidx.compose.ui.unit.Dp',
'import androidx.compose.ui.unit.dp',
'import org.breezyweather.ui.expressive.theme.Sizes',
'',
'/**',
' * Every icon Solstice uses, from Material Symbols **Rounded** (weight 400, grade 0), so call sites',
' * never pick a source or a style. Rules (docs/PLAN.md, "Icons"):',
' * - Unfilled by default; the `…Fill` variants only for on/selected (starred, pinned, location in use).',
' * - Optical size follows the drawn size, as Material recommends: `SolsticeIcons.X` is drawn for',
' *   24 dp, `SolsticeIcons.Small.X` for 20 dp and smaller ([forSize] picks one).',
' * - New icon: add both optical sizes here from google/material-design-icons',
' *   (`symbols/web/<name>/materialsymbolsrounded/<name>_24px.svg` and `_20px.svg`). Don\'t use',
' *   `material-icons-extended` (the old Material Icons, frozen since 2022, removed).',
' *',
' * Material Symbols: Apache License 2.0, Google.',
' */',
'object SolsticeIcons {',
]
def one(indent, name, p, mir):
    chunks=[p[i:i+88] for i in range(0,len(p),88)]
    r=[f'{indent}val {name}: ImageVector by lazy {{', f'{indent}    symbol(', f'{indent}        "{name}",']
    for i,c in enumerate(chunks):
        pre = f'{indent}        ' if i==0 else f'{indent}            '
        post = ' +' if i < len(chunks)-1 else (',' if mir else '')
        r.append(f'{pre}"{c}"{post}')
    if mir: r.append(f'{indent}        autoMirror = true')
    r += [f'{indent}    )', f'{indent}}}']
    return r
def entries(d, indent):
    res=[]
    for n in syms:
        res += one(indent, pascal(n), path(d,n), n in mirror)
    for n in fills:
        res += one(indent, pascal(n)+'Fill', path(d,n+'_fill'), False)
    return res
allnames=[pascal(n) for n in syms]+[pascal(n)+'Fill' for n in fills]
out += entries('svg','    ')
out += ['', '    /** The same icons at optical size 20, for icons drawn at 20 dp or smaller. */', '    object Small {']
out += entries('svg20','        ')
out += ['    }', '',
'    /**',
'     * The variant of [icon] drawn best at [size]: the optical size 20 version ([Small]) up to 20 dp,',
'     * as Material recommends. Icons from outside the catalog are returned as they are.',
'     */',
'    fun forSize(icon: ImageVector, size: Dp): ImageVector =',
'        if (size <= Sizes.Icon) small[icon.name]?.invoke() ?: icon else icon',
'',
'    private val small: Map<String, () -> ImageVector> = mapOf(',
] + [f'        "{n}" to {{ Small.{n} }}' + (',' if i < len(allnames)-1 else '') for i,n in enumerate(allnames)] + [
'    )',
'}', '',
'/** One Material Symbol (viewport 960, y from -960 to 0 as in the SVGs). */',
'private fun symbol(name: String, path: String, autoMirror: Boolean = false): ImageVector =',
'    ImageVector.Builder(',
'        name = name,',
'        defaultWidth = 24.dp,',
'        defaultHeight = 24.dp,',
'        viewportWidth = 960f,',
'        viewportHeight = 960f,',
'        autoMirror = autoMirror',
'    ).apply {',
'        group(translationY = 960f) {',
'            addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))',
'        }',
'    }.build()', '']
F.write_text('\n'.join(out))
print(len(syms), 'symbols')
