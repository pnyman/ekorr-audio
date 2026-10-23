/*
 * Sökning i innehållsfilen (ContentView). Läses in efter varje laddning.
 *
 * Varje träff läggs i ett <mark class="ekorr-search">. Det delar bara
 * textnoder – elementen med id (synkpunkterna) lämnas orörda, så
 * markering och klick fungerar som vanligt. clear() tar bort alla <mark>
 * och lagar textnoderna igen.
 *
 * Söker inte över elementgränser: en träff måste ligga i en och samma
 * textnod (t.ex. inte delvis inne i <em>).
 */
window.ekorrSearch = (function () {
    var MARK = 'ekorr-search';
    var CURRENT = 'ekorr-search-current';
    var marks = [];
    var current = -1;

    function clear() {
        var old = document.querySelectorAll('mark.' + MARK);
        for (var i = 0; i < old.length; i++) {
            var m = old[i];
            var parent = m.parentNode;
            parent.replaceChild(document.createTextNode(m.textContent), m);
            parent.normalize();   // slå ihop de delade textnoderna igen
        }
        marks = [];
        current = -1;
    }

    /** Söker (utan hänsyn till skiftläge) och returnerar antalet träffar. */
    function run(query) {
        clear();
        if (!query || !document.body) {
            return 0;
        }
        var needle = query.toLocaleLowerCase();

        // Samla textnoderna först – DOM:en ändras när träffarna läggs in
        var nodes = [];
        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode: function (n) {
                var p = n.parentNode;
                if (!p || p.nodeName === 'SCRIPT' || p.nodeName === 'STYLE') {
                    return NodeFilter.FILTER_REJECT;
                }
                return n.nodeValue.length >= needle.length
                    ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP;
            }
        });
        while (walker.nextNode()) {
            nodes.push(walker.currentNode);
        }

        for (var k = 0; k < nodes.length; k++) {
            var node = nodes[k];
            var text = node.nodeValue.toLocaleLowerCase();
            var hits = [];
            var at = text.indexOf(needle);
            while (at >= 0) {
                hits.push(at);
                at = text.indexOf(needle, at + needle.length);
            }
            // Bakifrån, så att tidigare positioner i noden inte flyttas
            var inNode = [];
            for (var h = hits.length - 1; h >= 0; h--) {
                node.splitText(hits[h] + needle.length);
                var middle = node.splitText(hits[h]);
                var mark = document.createElement('mark');
                mark.className = MARK;
                middle.parentNode.replaceChild(mark, middle);
                mark.appendChild(middle);
                inNode.unshift(mark);
            }
            marks = marks.concat(inNode);
        }
        return marks.length;
    }

    /** Gör träff i (0-baserat, räknas runt) till aktuell och scrollar dit. */
    function go(i) {
        if (marks.length === 0) {
            return -1;
        }
        if (current >= 0 && current < marks.length) {
            marks[current].classList.remove(CURRENT);
        }
        current = ((i % marks.length) + marks.length) % marks.length;
        var m = marks[current];
        m.classList.add(CURRENT);
        var r = m.getBoundingClientRect();
        if (r.top < 0 || r.bottom > window.innerHeight) {
            m.scrollIntoView({ behavior: 'auto', block: 'center' });
        }
        return current;
    }

    return {
        run: run,
        clear: clear,
        go: go,
        next: function () { return go(current + 1); },
        previous: function () { return go(current < 0 ? marks.length - 1 : current - 1); },
        count: function () { return marks.length; },
        current: function () { return current; }
    };
})();
