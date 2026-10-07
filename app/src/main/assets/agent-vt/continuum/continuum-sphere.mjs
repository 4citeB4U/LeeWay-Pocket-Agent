// #region LEEWAY_SOURCE_IDENTITY
// TAG: LEEWAY-CONTINUUM-PORTABLE-SPHERE-CANDIDATE
// WHO: Creator-authorized Agent Lee implementation; host supplies authenticated data.
// WHAT: A dependency-free Canvas 2D port of the Creator-supplied Continuum visual.
// WHERE: Existing Agent VT/Pocket surface; isolated candidate pending host admission.
// WHEN: 2026-10-07 UTC.
// WHY: Reuse the approved honeycomb visual as a view of actual Continuum records.
// HOW: Source-derived geometry and rendering; injected data and selection callbacks.
// AUTHORIZED ROLES: Creator approves scope; Agent Lee implements; host owns authority.
// LICENSE: No new license grant asserted. Uploaded source has no license declaration;
// retain applicable canonical repository licensing when this candidate is admitted.
// This module owns no database, agent, Formula, transport, or verification authority.
// SOURCE ARCHIVE SHA-256:
// ebdadd269083121c57f8a49842f519959f7bd956d512cba4a1b8c483f95d1b38
// SOURCE: app/src/main/java/com/example/engine3d/GeodesicHoneycombMesh.kt
// SHA-256: fb849ed969617519f53721416c5688daae853d3f7ffbb8a80be9b9228c9e89cf
// SOURCE: app/src/main/java/com/example/engine3d/Continuum3DView.kt
// SHA-256: 8b948c60d0484ca0813f391a3827adcf78a4177ac8c9bac220f3026acab77cd3
// ALSO: engine3d/Vector3D.kt and engine3d/HoneycombGeometry.kt in the same archive.
// #endregion

export const SOURCE_ARCHIVE_SHA256 = 'ebdadd269083121c57f8a49842f519959f7bd956d512cba4a1b8c483f95d1b38';

// #region SOURCE_GEOMETRY
const F = Math.fround;
const TAU = Math.PI * 2;
const clamp = (n, lo, hi) => Math.max(lo, Math.min(hi, n));
const add = (a, b) => a.map((v, i) => F(v + b[i]));
const sub = (a, b) => a.map((v, i) => F(v - b[i]));
const mul = (a, k) => a.map(v => F(v * F(k)));
const dot = (a, b) => F(F(F(a[0] * b[0]) + F(a[1] * b[1])) + F(a[2] * b[2]));
const cross = (a, b) => [F(F(a[1] * b[2]) - F(a[2] * b[1])), F(F(a[2] * b[0]) - F(a[0] * b[2])), F(F(a[0] * b[1]) - F(a[1] * b[0]))];
const length = a => F(Math.sqrt(dot(a, a)));
const normalize = a => { const n = length(a); return n > 0.0001 ? a.map(v => F(v / n)) : [0, 0, 1]; };
const rotateX = (v, angle) => {
  const c = F(Math.cos(F(angle))), s = F(Math.sin(F(angle)));
  return [v[0], F(F(v[1] * c) - F(v[2] * s)), F(F(v[1] * s) + F(v[2] * c))];
};
const rotateY = (v, angle) => {
  const c = F(Math.cos(F(angle))), s = F(Math.sin(F(angle)));
  return [F(F(v[0] * c) + F(v[2] * s)), v[1], F(F(-v[0] * s) + F(v[2] * c))];
};
const rotate = (v, yaw, pitch) => rotateX(rotateY(v, yaw), pitch);
const tangent = normal => {
  const up = Math.abs(normal[1]) > 0.9 ? [1, 0, 0] : [0, 1, 0];
  const u = normalize(cross(normal, up));
  return [u, normalize(cross(normal, u))];
};

// Exact mesh palette and anchor order from GeodesicHoneycombMesh.kt:173–202.
export const UNIVERSES = Object.freeze([
  ['work', 'WORK', '#FF1744', '#FF8A80', [-0.62, 0.45, 0.50]],
  ['places', 'PLACES', '#FFD600', '#FFFF8D', [-0.65, -0.42, 0.45]],
  ['people', 'PEOPLE', '#00E676', '#B9F6CA', [0.05, -0.85, 0.42]],
  ['knowledge', 'KNOWLEDGE', '#D500F9', '#EA80FC', [0.72, -0.15, 0.55]],
  ['time', 'TIME', '#00B0FF', '#80D8FF', [0.05, 0.85, 0.42]],
  ['experience', 'EXPERIENCE', '#FF6D00', '#FFD180', [0.65, 0.60, -0.35]],
  ['devices', 'DEVICES', '#00E5FF', '#84FFFF', [-0.65, -0.60, -0.35]],
  ['finance', 'FINANCE', '#FF3D00', '#FF9E80', [0.55, -0.62, 0.35]],
  ['health', 'HEALTH', '#76FF03', '#CCFF90', [-0.35, -0.78, -0.32]],
  ['media', 'MEDIA', '#FF4081', '#FF80AB', [0.35, 0.78, -0.32]],
  ['network', 'NETWORK', '#3D5AFE', '#8C9EFF', [-0.80, 0.15, -0.42]],
  ['creative', 'CREATIVE', '#1DE9B6', '#A7FFEB', [0.80, 0.20, -0.42]],
  ['security', 'SECURITY', '#F50057', '#FF80AB', [0.0, -0.22, -0.88]],
].map(([id, label, color, glowColor, anchor]) => Object.freeze({ id, label, color, glowColor, anchor: Object.freeze(normalize(anchor.map(F))) })));
const universeById = new Map(UNIVERSES.map(u => [u.id, u]));
const CENTER_PALETTE = Object.freeze({ color: '#E0F7FA', glowColor: '#FFFFFF' });

/** Deterministic source-derived topology. The 92 cells are visual slots, not a data limit. */
export function createGeodesicGeometry() {
  const phi = F(F(1 + F(Math.sqrt(5))) / 2);
  const len = F(Math.sqrt(F(1 + F(phi * phi))));
  const base = [[-1, phi, 0], [1, phi, 0], [-1, -phi, 0], [1, -phi, 0], [0, -1, phi], [0, 1, phi], [0, -1, -phi], [0, 1, -phi], [phi, 0, -1], [phi, 0, 1], [-phi, 0, -1], [-phi, 0, 1]].map(v => v.map(x => F(x / len)));
  const faces = [[0,11,5],[0,5,1],[0,1,7],[0,7,10],[0,10,11],[1,5,9],[5,11,4],[11,10,2],[10,7,6],[7,1,8],[3,9,4],[3,4,2],[3,2,6],[3,6,8],[3,8,9],[4,9,5],[2,4,11],[6,2,10],[8,6,7],[9,8,1]];
  const vertices = [], vertexMap = new Map(), triangles = [];
  function vertex(fi, a, b, c, i, j, k) {
    let key;
    if (i === 3) key = `v_${a}`;
    else if (j === 3) key = `v_${b}`;
    else if (k === 3) key = `v_${c}`;
    else if (k === 0) key = `e_${Math.min(a,b)}_${Math.max(a,b)}_${a < b ? i : j}`;
    else if (j === 0) key = `e_${Math.min(a,c)}_${Math.max(a,c)}_${a < c ? i : k}`;
    else if (i === 0) key = `e_${Math.min(b,c)}_${Math.max(b,c)}_${b < c ? j : k}`;
    else key = `f_${fi}_${i}_${j}_${k}`;
    if (!vertexMap.has(key)) {
      vertexMap.set(key, vertices.length);
      vertices.push(normalize(add(add(mul(base[a], F(i / 3)), mul(base[b], F(j / 3))), mul(base[c], F(k / 3)))));
    }
    return vertexMap.get(key);
  }
  faces.forEach(([a,b,c], fi) => {
    for (let row = 0; row < 3; row++) for (let col = 0; col < 3 - row; col++) {
      const i = 3 - row - col, j = col, k = row;
      const p0 = vertex(fi,a,b,c,i,j,k), p1 = vertex(fi,a,b,c,i-1,j+1,k), p2 = vertex(fi,a,b,c,i-1,j,k+1);
      triangles.push([p0,p1,p2]);
      if (i - 1 > 0) triangles.push([p1,vertex(fi,a,b,c,i-2,j+1,k+1),p2]);
    }
  });
  const v5 = vertices[vertexMap.get('v_5') ?? 0];
  // Preserve the implementation's negative angle, even though its comment says +Z.
  const sourceRotationX = F(-F(Math.atan2(v5[1], v5[2])));
  const rotated = vertices.map(v => normalize(rotateX(v, sourceRotationX)));
  // Source uses normalized centroid sums; do not replace these with true circumcenters.
  const corners = triangles.map(([a,b,c]) => normalize(add(add(rotated[a], rotated[b]), rotated[c])));
  let centerIndex = 0;
  for (let i = 1; i < rotated.length; i++) if (rotated[i][2] > rotated[centerIndex][2]) centerIndex = i;
  const counts = new Map();
  const cells = rotated.map((center, index) => {
    const incident = [], neighbors = new Set();
    triangles.forEach((tri, ti) => { if (tri.includes(index)) { incident.push(ti); tri.forEach(n => { if (n !== index) neighbors.add(n); }); } });
    const [u,v] = tangent(center);
    incident.sort((a,b) => F(Math.atan2(dot(corners[a],v),dot(corners[a],u))) - F(Math.atan2(dot(corners[b],v),dot(corners[b],u))));
    let universeId = 'continuum', rank = 0;
    if (index !== centerIndex) {
      let bestDot = -2;
      for (const domain of UNIVERSES) {
        const value = dot(center, domain.anchor);
        if (value > bestDot) { bestDot = value; universeId = domain.id; }
      }
      rank = counts.get(universeId) ?? 0;
      counts.set(universeId, rank + 1);
    }
    return { index, id: index === centerIndex ? 'continuum' : `${universeId}_${rank}`, universeId, rank, isCenter: index === centerIndex, isPrimary: index === centerIndex || rank === 0, center: Object.freeze(center), cornerIndices: Object.freeze(incident), neighborIndices: Object.freeze([...neighbors].sort((a,b) => a-b)) };
  });
  // Source adjacency indices are retained; resolve IDs in a second pass so no dangling
  // "cell_index" aliases survive the source's otherwise correct neighbor topology.
  for (const cell of cells) {
    cell.boundary = Object.freeze(cell.cornerIndices.map(i => Object.freeze(corners[i])));
    cell.neighborIds = Object.freeze(cell.neighborIndices.map(i => cells[i].id));
    Object.freeze(cell);
  }
  return Object.freeze({ cells: Object.freeze(cells), corners: Object.freeze(corners.map(Object.freeze)), triangles: Object.freeze(triangles.map(Object.freeze)), centerIndex, sourceRotationX, sourceArchiveSha256: SOURCE_ARCHIVE_SHA256 });
}
const GEOMETRY = createGeodesicGeometry();
export const SPHERE_ITEM_CAPACITY = Object.freeze(Object.fromEntries(UNIVERSES.map(u => [u.id, GEOMETRY.cells.filter(c => c.universeId === u.id && !c.isPrimary).length])));
// #endregion

// #region DATA_BOUNDARY
/**
 * Input: {universes:[{id,label?,count:null|integer,items:[{id,label}]}]}.
 * count=null/omitted means unknown; count=0 means known empty. Counts are never
 * inferred from loaded items. Item slots show only caller-supplied labels/IDs.
 * Only the visible slots are retained; the host owns full record lists/pagination.
 * No data is persisted, fetched, sent to an agent, or labeled verified here.
 */
export function normalizeSphereData(input = {}) {
  const rows = Array.isArray(input) ? input : input?.universes ?? [];
  if (!Array.isArray(rows)) throw new TypeError('universes must be an array');
  const result = new Map();
  for (const row of rows) {
    if (!row || !universeById.has(row.id)) throw new TypeError('Unknown Continuum universe ID');
    if (result.has(row.id)) throw new TypeError(`Duplicate universe ID: ${row.id}`);
    if (row.count != null && (!Number.isSafeInteger(row.count) || row.count < 0)) throw new TypeError('count must be a nonnegative integer or null');
    if (row.items != null && !Array.isArray(row.items)) throw new TypeError('items must be an array');
    const seen = new Set();
    const items = (row.items ?? []).slice(0, SPHERE_ITEM_CAPACITY[row.id]).map(item => {
      if (!item || typeof item.id !== 'string' || !item.id.trim() || seen.has(item.id)) throw new TypeError('Items require unique nonempty IDs within a universe');
      seen.add(item.id);
      const label = item.label ?? item.title ?? '';
      if (typeof label !== 'string') throw new TypeError('Item label must be a string');
      return Object.freeze({ id: item.id, label });
    });
    if (row.label != null && typeof row.label !== 'string') throw new TypeError('Universe label must be a string');
    result.set(row.id, Object.freeze({ label: row.label || universeById.get(row.id).label, count: row.count ?? null, items: Object.freeze(items) }));
  }
  return result;
}

function visualCell(cell, data, selection) {
  const domain = universeById.get(cell.universeId), row = data.get(cell.universeId);
  const item = !cell.isPrimary ? row?.items[cell.rank - 1] : null;
  const active = selection === cell.universeId;
  return { ...cell, color: domain?.color ?? CENTER_PALETTE.color, glowColor: domain?.glowColor ?? CENTER_PALETTE.glowColor,
    label: cell.isCenter ? 'CONTINUUM' : cell.isPrimary ? row?.label ?? domain.label : item?.label ?? '',
    recordId: item?.id ?? null, count: cell.isPrimary && !cell.isCenter ? row?.count ?? null : null,
    active, scaleMultiplier: cell.isCenter ? 1.05 : active ? 1.13 : 0.98 };
}
// #endregion

// #region PROJECTION_AND_DRAWING
function project(v, camera) {
  // Vector3D.kt:94–108. Positive Z and cameraZ+Z are deliberately preserved.
  const scale = camera.focal / Math.max(camera.minimumZ, camera.z + v[2]);
  return { x: camera.w / 2 + v[0] * scale, y: camera.h / 2 + v[1] * scale, scale, z: v[2] };
}
function projectCell(cell, camera, radius, yaw, pitch, elevation, units) {
  const normal = normalize(rotate(cell.center,yaw,pitch));
  const effectiveRadius = radius * (1 + (cell.active ? elevation : 0) * 0.38);
  const extrusion = (cell.active ? 10 : 6) * cell.scaleMultiplier * units;
  const center3d = mul(normal,effectiveRadius + extrusion), center = project(center3d,camera);
  const inset = cell.active ? 0.05 : 0.075;
  const base = [], top = [];
  for (const boundary of cell.boundary) {
    const direction = normalize(rotate(boundary,yaw,pitch));
    base.push(project(mul(direction,effectiveRadius),camera));
    const blend = normalize(add(mul(direction,1-inset),mul(normal,inset)));
    top.push(project(mul(blend,effectiveRadius+extrusion),camera));
  }
  const approximateRadius = Math.hypot(top[0].x-center.x,top[0].y-center.y) / center.scale;
  const microRadius = approximateRadius * 0.22, [u,v] = tangent(normal);
  const micro = [];
  for (let ring = -1; ring < 6; ring++) {
    const angle = ring * Math.PI / 3;
    const cellCenter = ring < 0 ? center3d : add(center3d,mul(add(mul(u,Math.cos(angle)),mul(v,Math.sin(angle))),approximateRadius*0.44));
    const points = [];
    for (let k=0;k<6;k++) {
      const a = k*Math.PI/3 + Math.PI/6;
      points.push(project(add(cellCenter,mul(add(mul(u,Math.cos(a)),mul(v,Math.sin(a))),microRadius)),camera));
    }
    micro.push(points);
  }
  return { cell, center, base, top, micro, normalZ: normal[2], alpha: cell.active ? 1 : clamp((normal[2]+0.40)/1.40,0.28,1), depth: center.z, scale: center.scale };
}
const rgbCache = new Map();
function rgba(hex, alpha = 1) {
  let rgb = rgbCache.get(hex);
  if (!rgb) { rgb = [1,3,5].map(i => parseInt(hex.slice(i,i+2),16)); rgbCache.set(hex,rgb); }
  return `rgba(${rgb.join(',')},${clamp(alpha,0,1)})`;
}
function path(ctx, vertices) {
  ctx.beginPath(); ctx.moveTo(vertices[0].x,vertices[0].y);
  for (let i=1;i<vertices.length;i++) ctx.lineTo(vertices[i].x,vertices[i].y);
  ctx.closePath();
}
function polygon(ctx, vertices, fill, stroke, width=1) {
  path(ctx,vertices);
  if (fill) { ctx.fillStyle=fill; ctx.fill(); }
  if (stroke && width>0) { ctx.strokeStyle=stroke; ctx.lineWidth=width; ctx.lineJoin='round'; ctx.stroke(); }
}
function circle(ctx,x,y,r,fill,stroke,width=1) {
  if (!(r>0)) return;
  ctx.beginPath(); ctx.arc(x,y,r,0,TAU);
  if (fill) { ctx.fillStyle=fill; ctx.fill(); }
  if (stroke) { ctx.strokeStyle=stroke; ctx.lineWidth=width; ctx.stroke(); }
}
function line(ctx,a,b,color,width=1) {
  ctx.beginPath();ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);ctx.strokeStyle=color;ctx.lineWidth=width;ctx.lineCap='round';ctx.stroke();
}
function roundedRect(ctx,x,y,w,h,r,color,width) {
  ctx.beginPath();ctx.moveTo(x+r,y);ctx.lineTo(x+w-r,y);ctx.quadraticCurveTo(x+w,y,x+w,y+r);ctx.lineTo(x+w,y+h-r);ctx.quadraticCurveTo(x+w,y+h,x+w-r,y+h);ctx.lineTo(x+r,y+h);ctx.quadraticCurveTo(x,y+h,x,y+h-r);ctx.lineTo(x,y+r);ctx.quadraticCurveTo(x,y,x+r,y);ctx.closePath();ctx.strokeStyle=color;ctx.lineWidth=width;ctx.stroke();
}
function icon(ctx,type,x,y,s,color) {
  const p=(dx,dy)=>({x:x+dx*s,y:y+dy*s});
  if(type==='continuum') {
    circle(ctx,x,y,14*s,null,'#00E5FF',3.8*s);ctx.beginPath();ctx.arc(x,y,14*s,220*Math.PI/180,320*Math.PI/180);ctx.strokeStyle='#00E676';ctx.lineWidth=4.2*s;ctx.stroke();circle(ctx,x,y,3.5*s,'white');
  } else if(type==='people') {
    circle(ctx,x,y-5*s,4*s,color);ctx.beginPath();ctx.ellipse(x,y+4*s,7.5*s,5*s,0,Math.PI,TAU);ctx.closePath();ctx.fillStyle=color;ctx.fill();circle(ctx,x+7*s,y-3*s,3*s,color);ctx.beginPath();ctx.ellipse(x+7*s,y+4*s,5*s,4*s,0,Math.PI,TAU);ctx.closePath();ctx.fill();
  } else if(type==='knowledge') {
    ctx.beginPath();ctx.moveTo(x,y+4*s);ctx.bezierCurveTo(x-4*s,y+s,x-10*s,y+s,x-12*s,y+4*s);ctx.lineTo(x-12*s,y-6*s);ctx.bezierCurveTo(x-10*s,y-8*s,x-4*s,y-8*s,x,y-5*s);ctx.bezierCurveTo(x+4*s,y-8*s,x+10*s,y-8*s,x+12*s,y-6*s);ctx.lineTo(x+12*s,y+4*s);ctx.bezierCurveTo(x+10*s,y+s,x+4*s,y+s,x,y+4*s);ctx.closePath();ctx.strokeStyle=color;ctx.lineWidth=2.2*s;ctx.stroke();line(ctx,p(0,-5),p(0,4),color,2*s);
  } else if(type==='experience') {
    const points=Array.from({length:10},(_,i)=>{const r=(i%2?4.2:9)*s,a=i*Math.PI/5-Math.PI/2;return{x:x+r*Math.cos(a),y:y+r*Math.sin(a)};});polygon(ctx,points,color);
  } else if(type==='time') {
    circle(ctx,x,y,8.5*s,null,color,2.2*s);line(ctx,p(0,0),p(0,-5.5),color,2.2*s);line(ctx,p(0,0),p(4,0),color,2.2*s);
  } else if(type==='work') {
    roundedRect(ctx,x-9*s,y-4*s,18*s,12*s,2.5*s,color,2.2*s);roundedRect(ctx,x-4*s,y-8*s,8*s,4*s,1.5*s,color,2*s);
  } else if(type==='places') {
    ctx.beginPath();ctx.moveTo(x,y+8*s);ctx.bezierCurveTo(x-6*s,y+2*s,x-5*s,y-5*s,x-5*s,y-2*s);ctx.arc(x,y-2*s,5*s,Math.PI,TAU);ctx.bezierCurveTo(x+5*s,y-5*s,x+6*s,y+2*s,x,y+8*s);ctx.closePath();ctx.fillStyle=color;ctx.fill();circle(ctx,x,y-2*s,2.2*s,'black');
  } else if(type==='devices') {
    roundedRect(ctx,x-8.5*s,y-6.5*s,17*s,11*s,2*s,color,2.2*s);line(ctx,p(0,4.5),p(0,8.5),color,2*s);line(ctx,p(-4,8.5),p(4,8.5),color,2.2*s);
  } else circle(ctx,x,y,5*s,color);
}
function javaHash(text) { let hash=0;for(let i=0;i<text.length;i++)hash=(Math.imul(hash,31)+text.charCodeAt(i))|0;return hash; }

function drawPlate(ctx,hex,phase,units) {
  const n=hex.cell,a=hex.alpha,s=hex.scale*units,front=hex.normalZ>-0.05;
  polygon(ctx,hex.base,null,rgba(n.glowColor,(n.active?0.75:0.30)*a),(n.active?12:5)*s);
  for(let i=0;i<hex.top.length;i++) {
    const j=(i+1)%hex.top.length,light=clamp(Math.cos(i*TAU/hex.top.length-0.8)*0.45+0.55,0.2,1);
    polygon(ctx,[hex.base[i],hex.base[j],hex.top[j],hex.top[i]],light>0.65?rgba(n.glowColor,0.60*a*light):rgba('#030D18',0.85*a),rgba(n.color,0.40*a),s);
  }
  const radius=Math.max(1,Math.hypot(hex.top[0].x-hex.center.x,hex.top[0].y-hex.center.y)*1.3);
  const gradient=ctx.createRadialGradient(hex.center.x,hex.center.y,0,hex.center.x,hex.center.y,radius);
  gradient.addColorStop(0,rgba(n.glowColor,(front?0.88:0.50)*a));gradient.addColorStop(0.5,rgba(n.color,(front?0.65:0.35)*a));gradient.addColorStop(1,rgba('#040F1D',(front?0.90:0.70)*a));
  polygon(ctx,hex.top,gradient);
  if(front&&a>0.35)for(const micro of hex.micro)polygon(ctx,micro,null,rgba(n.glowColor,0.38*a),0.9*s);
  if(n.active) {
    const seed=Math.abs(javaHash(n.id)),pulse=Math.sin(phase*3.5+(seed%8)*0.4);
    polygon(ctx,hex.top,null,rgba(n.glowColor,(0.75+0.25*pulse)*a*0.70),Math.max(units,(5.5+2.5*pulse)*s));
    polygon(ctx,hex.top,null,rgba(n.glowColor,a),Math.max(units,3.2*s));
    ctx.setLineDash([Math.max(units,14*s),Math.max(units,8*s)]);ctx.lineDashOffset=(phase*55+(seed%12)*8)*units;
    polygon(ctx,hex.top,null,rgba('#FFFFFF',a),Math.max(units,3.6*s));ctx.setLineDash([]);ctx.lineDashOffset=0;
    const cycle=((phase*2.2+(seed%6)*0.8)%hex.top.length+hex.top.length)%hex.top.length,i=Math.floor(cycle),t=cycle-i,p=hex.top[i],q=hex.top[(i+1)%hex.top.length];
    const x=p.x+(q.x-p.x)*t,y=p.y+(q.y-p.y)*t;
    circle(ctx,x,y,Math.max(units,5.5*s),rgba(n.glowColor,0.85*a));circle(ctx,x,y,Math.max(0.8*units,2.6*s),rgba('#FFFFFF',a));
  } else polygon(ctx,hex.top,null,rgba(n.isCenter?'#00E5FF':n.glowColor,(n.isCenter?1:0.70)*a),Math.max(units,(n.isCenter?3.5:2.4)*s));
  if(front) { circle(ctx,hex.top[0].x,hex.top[0].y,2.6*s,rgba('#FFFFFF',0.85*a));circle(ctx,hex.top[1].x,hex.top[1].y,2*s,rgba('#FFFFFF',0.60*a)); }
  if(!front||a<=0.4)return;
  const contentScale=s*(n.active?1.25:1)*n.scaleMultiplier,satellite=!n.isPrimary;
  if(n.isPrimary||n.recordId)icon(ctx,n.isCenter?'continuum':satellite?'tile':n.universeId,hex.center.x,hex.center.y-(satellite?6:12)*contentScale,contentScale*(satellite?0.9:1),n.active?'white':n.color);
  if(n.label) {
    ctx.save();ctx.font=`700 ${(satellite?10.5:n.isCenter?14:12.5)*contentScale}px system-ui, sans-serif`;ctx.textAlign='center';ctx.textBaseline='alphabetic';ctx.fillStyle='white';ctx.shadowColor=n.glowColor;ctx.shadowBlur=6*contentScale;
    const textY=hex.center.y+(satellite?8:12)*contentScale;
    ctx.fillText(n.label,hex.center.x,textY,Math.max(1,radius*1.45));ctx.shadowBlur=0;
    if(n.count!==null) {ctx.font=`${9.5*contentScale}px system-ui, sans-serif`;ctx.fillStyle='#80D8FF';ctx.fillText(String(n.count),hex.center.x,textY+12*contentScale,Math.max(1,radius*1.4));}
    ctx.restore();
  }
}

/* Source-count satellites: real connected record counts grow the visible color shell.
 * They are category occupancy indicators, not independent files or new data rows.
 */
export function sourceCapacitySlots(count){
 if(!Number.isSafeInteger(count)||count<=0)return 0;
 return Math.min(18,Math.max(0,Math.floor(Math.log2(count+1))-1));
}
function drawOccupancyCells(ctx,data,camera,radius,yaw,pitch,units,selectedUniverseId,elevation){
 const hits=[];
 for(const universe of UNIVERSES){
  const count=data.get(universe.id)?.count;
  if(!Number.isSafeInteger(count)||count<=0)continue;
  const growth=sourceCapacitySlots(count);
  if(!growth)continue;
  const [ux,vx]=tangent(universe.anchor);
  for(let i=0;i<growth;i++){
   const ring=Math.floor(i/9),angle=(i%9)*TAU/9+ring*.3;
   const d=.09+ring*.105;
   const dir=normalize(add(universe.anchor,add(mul(ux,Math.cos(angle)*d),mul(vx,Math.sin(angle)*d))));
   const turned=normalize(rotate(dir,yaw,pitch));
   if(turned[2]<-.08)continue;
   const active=selectedUniverseId===universe.id;
   const height=radius*(1+(active?elevation*.42:0)+.04+ring*.02);
   const p=project(mul(turned,height),camera);
   const size=Math.max(4.5,Math.min(17,13*units*p.scale*.55));
   const points=Array.from({length:6},(_,n)=>{const a=TAU*n/6+Math.PI/6;return{x:p.x+Math.cos(a)*size,y:p.y+Math.sin(a)*size};});
   const alpha=clamp((turned[2]+.1)/1.1,.22,.9);
   polygon(ctx,points,rgba(universe.color,(active?.44:.20)*alpha),rgba(universe.glowColor,(active?.92:.58)*alpha),Math.max(.8,1.3*units));
   hits.push({universeId:universe.id,points,normalZ:turned[2]});
  }
 }
 return hits;
}
/** Exact polygon hit-testing avoids the upload's circular hits crossing cell boundaries. */
export function pointInPolygon(x,y,points) {
  let inside=false;
  for(let i=0,j=points.length-1;i<points.length;j=i++) {
    const a=points[j],b=points[i],dx=b.x-a.x,dy=b.y-a.y;
    const crossValue=(x-a.x)*dy-(y-a.y)*dx;
    if(Math.abs(crossValue)<1e-5&&x>=Math.min(a.x,b.x)-1e-5&&x<=Math.max(a.x,b.x)+1e-5&&y>=Math.min(a.y,b.y)-1e-5&&y<=Math.max(a.y,b.y)+1e-5)return true;
    if((a.y>y)!==(b.y>y)&&x<(b.x-a.x)*(y-a.y)/(b.y-a.y)+a.x)inside=!inside;
  }
  return inside;
}
// FastOutSlowInEasing: cubic-bezier(0.4,0,0.2,1), as in the uploaded Compose tween.
function fastOutSlowIn(progress) {
  const x=clamp(progress,0,1);let low=0,high=1,t=x;
  for(let i=0;i<16;i++){const inv=1-t,bx=3*inv*inv*t*0.4+3*inv*t*t*0.2+t*t*t;if(bx<x)low=t;else high=t;t=(low+high)/2;}
  return 3*(1-t)*t*t+t*t*t;
}
// #endregion

// #region HOST_SURFACE_LIFECYCLE
/**
 * mountSphere(canvas, options) -> {setData, setMotion, selectUniverse, hitTest, renderNow,
 * getState, dispose}. Selection callbacks receive {cellId, universeId, recordId,
 * label, kind}. The host owns loading, errors, authentication, storage, and panels.
 * The uploaded camera is preserved at a 720-unit reference and scaled uniformly
 * for other viewports. sphereScale=1.25 reproduces the source's generous sizing.
 */
export function mountSphere(canvas, options = {}) {
  if(!canvas?.getContext)throw new TypeError('A canvas is required');
  if(options.onSelect!=null&&typeof options.onSelect!=='function')throw new TypeError('onSelect must be a function');
  for(const field of ['autoRotate','reducedMotion'])if(options[field]!==undefined&&typeof options[field]!=='boolean')throw new TypeError(`${field} must be a boolean`);
  if(options.selectedUniverseId!=null&&options.selectedUniverseId!=='continuum'&&!universeById.has(options.selectedUniverseId))throw new TypeError('Unknown Continuum universe selection');
  const ctx=canvas.getContext('2d');if(!ctx)throw new Error('Canvas 2D is unavailable');
  const win=canvas.ownerDocument?.defaultView??globalThis,doc=canvas.ownerDocument;
  const now=()=>win.performance?.now?.()??Date.now();
  const raf=win.requestAnimationFrame?.bind(win),caf=win.cancelAnimationFrame?.bind(win);
  const media=typeof options.reducedMotion==='boolean'?null:win.matchMedia?.('(prefers-reduced-motion: reduce)');
  let motionOverride=options.reducedMotion??null,reducedMotion=motionOverride??media?.matches??false;
  let autoRotate=options.autoRotate!==false;
  const sphereScale=clamp(Number(options.sphereScale)||1.25,0.5,1.4);
  const maxDpr=clamp(Number(options.maxDpr)||2,0.5,3),maxPixels=4_000_000;
  let data=normalizeSphereData(options.data),selectedUniverseId=null,disposed=false,frameId=null;
  let width=0,height=0,dpr=1,yaw=0.15,pitch=-0.1,yawVelocity=0,pitchVelocity=0;
  let lastTime=null,phase=0,elevation=0,transition=null,centering=null,pointer=null,projected=[],occupancyHits=[],frameCount=0;
  const previousStyles={touchAction:canvas.style?.touchAction,userSelect:canvas.style?.userSelect};
  const previousAttributes=new Map(['tabindex','role','aria-label'].map(name=>[name,canvas.getAttribute?.(name)]));
  if(canvas.style){canvas.style.touchAction='none';canvas.style.userSelect='none';}
  if(!canvas.hasAttribute?.('tabindex'))canvas.setAttribute?.('tabindex','0');
  if(!canvas.hasAttribute?.('role'))canvas.setAttribute?.('role','group');
  if(!canvas.hasAttribute?.('aria-label'))canvas.setAttribute?.('aria-label','Continuum honeycomb sphere. Drag or use arrow keys to rotate. Select a category to inspect its records.');
  const live=()=>!disposed&&doc?.hidden!==true&&width>0&&height>0;
  function cancelFrame(){if(frameId!==null){caf?.(frameId);frameId=null;}}
  function schedule(){if(!disposed&&doc?.hidden!==true&&frameId===null&&raf)frameId=raf(tick);}
  function resize(){
    if(disposed)return;
    const rect=canvas.getBoundingClientRect();const w=Math.max(0,rect.width),h=Math.max(0,rect.height);
    const ratio=Math.min(maxDpr,Math.max(0.5,Number(win.devicePixelRatio)||1),w&&h?Math.sqrt(maxPixels/(w*h)):maxDpr);
    if(w===width&&h===height&&ratio===dpr)return;
    width=w;height=h;dpr=ratio;canvas.width=Math.max(1,Math.round(w*dpr));canvas.height=Math.max(1,Math.round(h*dpr));projected=[];schedule();
  }
  function draw(){
    if(!live())return;
    ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,width,height);
    const units=Math.min(width,height)/720,radius=Math.min(width,height)*0.44*sphereScale;
    const camera={w:width,h:height,focal:850*units,z:950*units,minimumZ:100*units};
    const core=ctx.createRadialGradient(width/2,height/2,0,width/2,height/2,radius);
    core.addColorStop(0,rgba('#050D1A',224/255));core.addColorStop(1/3,rgba('#020710',176/255));core.addColorStop(2/3,rgba('#010408',96/255));core.addColorStop(1,'rgba(0,0,0,0)');
    circle(ctx,width/2,height/2,radius*0.96,core);
    projected=GEOMETRY.cells.map(cell=>projectCell(visualCell(cell,data,selectedUniverseId),camera,radius,yaw,pitch,elevation,units)).sort((a,b)=>a.depth-b.depth);
    for(const hex of projected)drawPlate(ctx,hex,phase,units);
    occupancyHits=drawOccupancyCells(ctx,data,camera,radius,yaw,pitch,units,selectedUniverseId,elevation);
    const active=projected.filter(h=>h.cell.active);
    for(let i=0;i<active.length;i++)for(let j=i+1;j<active.length;j++) {
      const a=active[i],b=active[j];
      if(length(sub(a.cell.center,b.cell.center))>=0.68||Math.max(a.normalZ,b.normalZ)<=-0.35)continue;
      const alpha=clamp((a.alpha+b.alpha)*0.45,0,1),s=a.scale*units;
      line(ctx,a.center,b.center,rgba(a.cell.glowColor,alpha),Math.max(units,2.8*s));
      const t=((phase+(i+j)*0.45)/TAU%1+1)%1;
      circle(ctx,a.center.x+(b.center.x-a.center.x)*t,a.center.y+(b.center.y-a.center.y)*t,Math.max(units,2.4*s),rgba('#FFFFFF',alpha));
    }
    // The upload calls only this selected-cell ring; its separate wireframe/orbit
    // helper is unmounted, so the port does not introduce extra orbit decorations.
    const first=active[0];
    if(first&&first.normalZ>-0.35){const s=first.scale*units;ctx.setLineDash([12*units,6*units]);ctx.lineDashOffset=phase*10*units;circle(ctx,first.center.x,first.center.y,Math.max(units,(48+Math.sin(phase)*6)*s),null,rgba(first.cell.glowColor,0.5),Math.max(units,2.5*s));ctx.setLineDash([]);ctx.lineDashOffset=0;}
    frameCount++;
  }
  function tick(time){
    frameId=null;if(!live())return;
    const dt=lastTime===null?0:clamp(time-lastTime,0,64);lastTime=time;
    if(!reducedMotion){
      phase=(phase+dt*TAU/4000)%TAU;
      if(transition){const t=clamp((time-transition.start)/550,0,1);elevation=fastOutSlowIn(t);if(t>=1)transition=null;}
      if(centering&&!pointer?.dragging){const t=clamp((time-centering.start)/600,0,1),e=fastOutSlowIn(t);yaw=centering.yaw+(centering.targetYaw-centering.yaw)*e;pitch=centering.pitch+(centering.targetPitch-centering.pitch)*e;if(t>=1)centering=null;}
      else if(!pointer?.dragging){
        if(autoRotate)yaw+=0.0025*dt/16;
        if(Math.abs(yawVelocity)>0.0001||Math.abs(pitchVelocity)>0.0001){yaw+=yawVelocity*dt/16;pitch=clamp(pitch+pitchVelocity*dt/16,-1.25,1.25);const decay=0.92**(dt/16);yawVelocity*=decay;pitchVelocity*=decay;}
      }
    }
    draw();
    if(!reducedMotion&&(autoRotate||selectedUniverseId!==null||transition||centering||Math.abs(yawVelocity)>0.0001||Math.abs(pitchVelocity)>0.0001))schedule();
  }
  function selectUniverse(id,{center=true,animate=true}={}){
    if(disposed)return;
    if(id!==null&&id!=='continuum'&&!universeById.has(id))throw new TypeError('Unknown Continuum universe selection');
    selectedUniverseId=id;yawVelocity=0;pitchVelocity=0;centering=null;transition=null;
    elevation=id===null?0:1;
    if(id!==null&&animate&&!reducedMotion){elevation=0;transition={start:now()};}
    if(id!==null&&center){
      const cluster=GEOMETRY.cells.filter(c=>c.universeId===id),target=normalize(cluster.reduce((a,c)=>add(a,c.center),[0,0,0]));
      let targetYaw=-Math.atan2(target[0],target[2]);const targetPitch=clamp(Math.atan2(target[1],Math.hypot(target[0],target[2])),-0.95,0.95);
      let delta=(targetYaw-yaw)%TAU;if(delta>Math.PI)delta-=TAU;if(delta< -Math.PI)delta+=TAU;targetYaw=yaw+delta;
      if(animate&&!reducedMotion)centering={start:now(),yaw,pitch,targetYaw,targetPitch};else{yaw=targetYaw;pitch=targetPitch;}
    }
    schedule();
  }
  function hitTest(x,y){
    if(disposed)return null;
    for(let i=occupancyHits.length-1;i>=0;i--){
      const cell=occupancyHits[i];
      if(cell.normalZ>0&&pointInPolygon(x,y,cell.points))
        return{cellId:'source-capacity:'+cell.universeId,universeId:cell.universeId,recordId:null,label:'Source-backed category capacity',kind:'universe'};
    }
    for(let i=projected.length-1;i>=0;i--){const h=projected[i];if(h.normalZ> -0.1&&pointInPolygon(x,y,h.top)){const n=h.cell;return{cellId:n.id,universeId:n.universeId,recordId:n.recordId,label:n.label,kind:n.isCenter?'continuum':n.recordId?'item':'universe'};}}
    return null;
  }
  const local=event=>{const r=canvas.getBoundingClientRect();return{x:event.clientX-r.left,y:event.clientY-r.top};};
  function pointerDown(event){
    if(disposed||event.isPrimary===false||(event.button!=null&&event.button!==0)||pointer)return;
    const p=local(event);pointer={id:event.pointerId,down:p,last:p,dragging:false,slop:event.pointerType==='touch'?8:4};
    try{canvas.setPointerCapture?.(event.pointerId);}catch{}
    canvas.focus?.({preventScroll:true});
  }
  function pointerMove(event){
    if(!pointer||pointer.id!==event.pointerId)return;
    const p=local(event),dx=p.x-pointer.last.x,dy=p.y-pointer.last.y;
    if(!pointer.dragging&&Math.hypot(p.x-pointer.down.x,p.y-pointer.down.y)>pointer.slop){pointer.dragging=true;centering=null;yawVelocity=0;pitchVelocity=0;}
    if(pointer.dragging){event.preventDefault?.();yaw+=dx*0.0065;pitch=clamp(pitch-dy*0.0065,-1.25,1.25);yawVelocity=reducedMotion?0:dx*0.0065*0.75;pitchVelocity=reducedMotion?0:-dy*0.0065*0.75;schedule();}
    pointer.last=p;
  }
  function pointerEnd(event,cancelled=false){
    if(!pointer||pointer.id!==event.pointerId)return;
    const saved=pointer,p=local(event);pointer=null;
    try{canvas.releasePointerCapture?.(event.pointerId);}catch{}
    if(!cancelled&&!saved.dragging&&Math.hypot(p.x-saved.down.x,p.y-saved.down.y)<=saved.slop){const selected=hitTest(p.x,p.y);if(selected){selectUniverse(selected.universeId);options.onSelect?.(Object.freeze(selected));}}
    schedule();
  }
  const pointerUp=e=>pointerEnd(e),pointerCancel=e=>pointerEnd(e,true);
  function keyDown(event){
    if(!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home'].includes(event.key))return;
    event.preventDefault?.();centering=null;yawVelocity=0;pitchVelocity=0;
    if(event.key==='Home'){yaw=0.15;pitch=-0.1;}
    else if(event.key==='ArrowLeft')yaw-=0.12;else if(event.key==='ArrowRight')yaw+=0.12;
    else pitch=clamp(pitch+(event.key==='ArrowUp'?0.08:-0.08),-1.25,1.25);
    schedule();
  }
  function applyMotionState(){
    cancelFrame();lastTime=null;
    if(reducedMotion){transition=null;centering=null;elevation=selectedUniverseId===null?0:1;yawVelocity=0;pitchVelocity=0;}
    // A paused surface may render once to settle its state. tick() will not queue
    // animation when reducedMotion is true; direct manipulation remains available.
    schedule();
  }
  function setMotion(update={}){
    if(disposed)return;
    if(!update||typeof update!=='object')throw new TypeError('Motion options must be an object');
    for(const field of ['autoRotate','reducedMotion'])if(update[field]!==undefined&&typeof update[field]!=='boolean')throw new TypeError(`${field} must be a boolean`);
    if(update.autoRotate!==undefined)autoRotate=update.autoRotate;
    if(update.reducedMotion!==undefined){motionOverride=update.reducedMotion;reducedMotion=update.reducedMotion;}
    applyMotionState();
  }
  function motionChange(event){if(motionOverride!==null)return;reducedMotion=event.matches;applyMotionState();}
  function visibilityChange(){cancelFrame();lastTime=null;if(doc?.hidden!==true)schedule();}
  const handlers=[['pointerdown',pointerDown],['pointermove',pointerMove],['pointerup',pointerUp],['pointercancel',pointerCancel],['lostpointercapture',pointerCancel],['keydown',keyDown]];
  for(const [name,handler]of handlers)canvas.addEventListener(name,handler);
  win.addEventListener?.('resize',resize);doc?.addEventListener?.('visibilitychange',visibilityChange);
  if(media?.addEventListener)media.addEventListener('change',motionChange);else media?.addListener?.(motionChange);
  const ResizeObserverType=win.ResizeObserver??globalThis.ResizeObserver;
  const observer=ResizeObserverType?new ResizeObserverType(resize):null;observer?.observe(canvas);
  resize();
  if(options.selectedUniverseId!=null)selectUniverse(options.selectedUniverseId,{animate:false});
  draw();schedule();
  return Object.freeze({
    setData(input){if(disposed)return;const next=normalizeSphereData(input);data=next;projected=[];draw();schedule();},
    setMotion,
    selectUniverse,
    hitTest,
    renderNow(){draw();},
    getState(){return Object.freeze({disposed,width,height,dpr,yaw,pitch,autoRotate,reducedMotion,selectedUniverseId,frameCount,cellCount:GEOMETRY.cells.length,sourceCountSatellites:occupancyHits.length,knownUniverseCount:data.size,mappedRecordCount:projected.filter(h=>h.cell.recordId!==null).length,pendingFrame:frameId!==null});},
    dispose(){if(disposed)return;disposed=true;cancelFrame();observer?.disconnect();for(const [name,handler]of handlers)canvas.removeEventListener(name,handler);win.removeEventListener?.('resize',resize);doc?.removeEventListener?.('visibilitychange',visibilityChange);if(media?.removeEventListener)media.removeEventListener('change',motionChange);else media?.removeListener?.(motionChange);if(pointer){try{canvas.releasePointerCapture?.(pointer.id);}catch{}pointer=null;}if(canvas.style){canvas.style.touchAction=previousStyles.touchAction??'';canvas.style.userSelect=previousStyles.userSelect??'';}for(const[name,value]of previousAttributes){if(value==null)canvas.removeAttribute?.(name);else canvas.setAttribute?.(name,value);}projected=[];occupancyHits=[];data.clear();},
  });
}
// #endregion
