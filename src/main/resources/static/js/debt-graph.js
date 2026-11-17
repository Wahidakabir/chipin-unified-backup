// Feature 1: draws the "before" and "after" debt graphs on the /debts page.
// Members are circles, debts are arrows. Hover to highlight, drag to rearrange.
(function () {
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var WIDTH = 480, HEIGHT = 360, NODE_R = 24;

    function el(name, attrs, text) {
        var node = document.createElementNS(SVG_NS, name);
        for (var key in attrs) node.setAttribute(key, attrs[key]);
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function money(amount) {
        return '৳' + Number(amount).toFixed(2);
    }

    // Reads the hidden <span> elements that the server filled in.
    function readData() {
        var members = [];
        document.querySelectorAll('#graph-data .g-member').forEach(function (s) {
            members.push({ name: s.dataset.name, balance: Number(s.dataset.balance) });
        });
        function edges(selector) {
            var list = [];
            document.querySelectorAll('#graph-data ' + selector).forEach(function (s) {
                list.push({ from: s.dataset.from, to: s.dataset.to, amount: Number(s.dataset.amount) });
            });
            return list;
        }
        return { members: members, before: edges('.g-before'), after: edges('.g-after') };
    }

    function drawGraph(svgId, infoId, members, edges) {
        var svg = document.getElementById(svgId);
        var info = document.getElementById(infoId);
        if (!svg) return;
        var defaultInfo = info ? info.textContent : '';

        // Arrow head
        var defs = el('defs', {});
        var marker = el('marker', {
            id: svgId + '-arrow', viewBox: '0 0 10 10', refX: '9', refY: '5',
            markerWidth: '12', markerHeight: '12', markerUnits: 'userSpaceOnUse', orient: 'auto'
        });
        marker.appendChild(el('path', { d: 'M 0 0 L 10 5 L 0 10 z', 'class': 'arrow-head' }));
        defs.appendChild(marker);
        svg.appendChild(defs);

        // Place members evenly around a circle.
        var pos = {};
        var cx = WIDTH / 2, cy = HEIGHT / 2, radiusX = 105, radiusY = 118;
        members.forEach(function (m, i) {
            var angle = -Math.PI / 2 + (2 * Math.PI * i) / members.length;
            pos[m.name] = { x: cx + radiusX * Math.cos(angle), y: cy + radiusY * Math.sin(angle) };
        });

        var largest = 0;
        edges.forEach(function (e) { largest = Math.max(largest, e.amount); });

        var edgeLayer = el('g', {});
        var nodeLayer = el('g', {});
        svg.appendChild(edgeLayer);
        svg.appendChild(nodeLayer);

        // Arrows: thicker line = bigger amount.
        var edgeViews = edges.filter(function (e) { return pos[e.from] && pos[e.to]; }).map(function (e) {
            var group = el('g', { 'class': 'edge' });
            var line = el('line', {
                'class': 'edge-line',
                'stroke-width': (1.5 + 3 * (largest ? e.amount / largest : 0)).toFixed(2),
                'marker-end': 'url(#' + svgId + '-arrow)'
            });
            var labelBg = el('rect', { 'class': 'edge-label-bg', rx: '4', height: '18' });
            var label = el('text', { 'class': 'edge-label', 'text-anchor': 'middle' }, money(e.amount));
            group.appendChild(line);
            group.appendChild(labelBg);
            group.appendChild(label);
            edgeLayer.appendChild(group);

            group.addEventListener('mouseenter', function () {
                highlight(function (other) { return other === view; }, [e.from, e.to]);
                if (info) info.textContent = e.from + ' pays ' + e.to + ' ' + money(e.amount);
            });
            group.addEventListener('mouseleave', clearHighlight);

            var view = { edge: e, group: group, line: line, label: label, labelBg: labelBg };
            return view;
        });

        // Member circles
        var nodeViews = members.map(function (m) {
            var state = m.balance > 0 ? 'owed' : (m.balance < 0 ? 'owing' : 'even');
            var group = el('g', { 'class': 'node node-' + state, tabindex: '0' });
            var circle = el('circle', { r: NODE_R, 'class': 'node-circle' });
            var initial = el('text', { 'class': 'node-initial', 'text-anchor': 'middle', dy: '0.35em' },
                m.name.charAt(0).toUpperCase());
            var name = el('text', { 'class': 'node-name', 'text-anchor': 'middle' }, m.name);
            var note = state === 'owed' ? 'is owed ' + money(m.balance)
                : state === 'owing' ? 'owes ' + money(-m.balance) : 'settled';
            var sub = el('text', { 'class': 'node-sub', 'text-anchor': 'middle' }, note);
            group.appendChild(circle);
            group.appendChild(initial);
            group.appendChild(name);
            group.appendChild(sub);
            nodeLayer.appendChild(group);

            function focusOn() {
                var linked = [m.name];
                highlight(function (v) {
                    var touches = v.edge.from === m.name || v.edge.to === m.name;
                    if (touches) linked.push(v.edge.from, v.edge.to);
                    return touches;
                }, linked);
                if (info) info.textContent = m.name + ' ' + note;
            }
            group.addEventListener('mouseenter', focusOn);
            group.addEventListener('focus', focusOn);
            group.addEventListener('mouseleave', clearHighlight);
            group.addEventListener('blur', clearHighlight);

            var view = { member: m, group: group, name: name, sub: sub };
            enableDrag(view);
            return view;
        });

        function highlight(edgeTest, memberNames) {
            edgeViews.forEach(function (v) { v.group.classList.toggle('dim', !edgeTest(v)); });
            nodeViews.forEach(function (v) {
                v.group.classList.toggle('dim', memberNames.indexOf(v.member.name) === -1);
            });
        }

        function clearHighlight() {
            edgeViews.forEach(function (v) { v.group.classList.remove('dim'); });
            nodeViews.forEach(function (v) { v.group.classList.remove('dim'); });
            if (info) info.textContent = defaultInfo;
        }

        // Positions everything from the current pos map (called again while dragging).
        function layout() {
            nodeViews.forEach(function (v) {
                var p = pos[v.member.name];
                v.group.setAttribute('transform', 'translate(' + p.x.toFixed(1) + ',' + p.y.toFixed(1) + ')');
                // Put the name on the outside of the ring, away from the arrows.
                var offX = p.x - cx, offY = p.y - cy;
                if (Math.abs(offX) > Math.abs(offY)) {
                    var right = offX > 0; // beside the circle
                    setLabel(v.name, right ? NODE_R + 8 : -(NODE_R + 8), -2, right ? 'start' : 'end');
                    setLabel(v.sub, right ? NODE_R + 8 : -(NODE_R + 8), 12, right ? 'start' : 'end');
                } else {
                    var below = offY >= 0; // above or below the circle
                    setLabel(v.name, 0, below ? NODE_R + 16 : -(NODE_R + 20), 'middle');
                    setLabel(v.sub, 0, below ? NODE_R + 30 : -(NODE_R + 6), 'middle');
                }
            });
            edgeViews.forEach(function (v) {
                var a = pos[v.edge.from], b = pos[v.edge.to];
                var dx = b.x - a.x, dy = b.y - a.y;
                var len = Math.sqrt(dx * dx + dy * dy) || 1;
                var ux = dx / len, uy = dy / len;
                // Start and stop at the edge of each circle, not its centre.
                v.line.setAttribute('x1', a.x + ux * (NODE_R + 2));
                v.line.setAttribute('y1', a.y + uy * (NODE_R + 2));
                v.line.setAttribute('x2', b.x - ux * (NODE_R + 4));
                v.line.setAttribute('y2', b.y - uy * (NODE_R + 4));
                // Label sits 30% of the way along, away from the middle where arrows cross.
                var lx = a.x + dx * 0.3, ly = a.y + dy * 0.3;
                var w = v.label.textContent.length * 7 + 10;
                v.label.setAttribute('x', lx);
                v.label.setAttribute('y', ly + 4);
                v.labelBg.setAttribute('x', lx - w / 2);
                v.labelBg.setAttribute('y', ly - 9);
                v.labelBg.setAttribute('width', w);
            });
        }

        function setLabel(text, x, y, anchor) {
            text.setAttribute('x', x);
            text.setAttribute('y', y);
            text.setAttribute('text-anchor', anchor);
        }

        function enableDrag(view) {
            var dragging = false;
            function toSvgPoint(evt) {
                var pt = svg.createSVGPoint();
                pt.x = evt.clientX;
                pt.y = evt.clientY;
                return pt.matrixTransform(svg.getScreenCTM().inverse());
            }
            view.group.addEventListener('pointerdown', function (evt) {
                dragging = true;
                view.group.setPointerCapture(evt.pointerId);
                evt.preventDefault();
            });
            view.group.addEventListener('pointermove', function (evt) {
                if (!dragging) return;
                var p = toSvgPoint(evt);
                pos[view.member.name] = {
                    x: Math.max(NODE_R, Math.min(WIDTH - NODE_R, p.x)),
                    y: Math.max(NODE_R, Math.min(HEIGHT - NODE_R, p.y))
                };
                layout();
            });
            function stop() { dragging = false; }
            view.group.addEventListener('pointerup', stop);
            view.group.addEventListener('pointercancel', stop);
        }

        if (edgeViews.length === 0) {
            svg.appendChild(el('text', { x: cx, y: cy + 5, 'class': 'empty-note', 'text-anchor': 'middle' },
                'No debts. All settled up!'));
        }
        layout();
    }

    document.addEventListener('DOMContentLoaded', function () {
        var data = readData();
        drawGraph('graph-before', 'info-before', data.members, data.before);
        drawGraph('graph-after', 'info-after', data.members, data.after);
    });
})();
