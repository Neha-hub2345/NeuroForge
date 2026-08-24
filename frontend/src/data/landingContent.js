import { KanbanSquare, GitBranch, Rocket, ShieldCheck, BarChart3, Boxes } from 'lucide-react'

export const fadeUp = {
  hidden: { opacity: 0, y: 22 },
  show: { opacity: 1, y: 0, transition: { duration: 0.55, ease: 'easeOut' } }
}

export const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.09 } }
}

export const FEATURES = [
  { Icon: KanbanSquare, title: 'Agile Kanban Boards', desc: 'Drag tasks across To Do, In Progress and Done with live story-point tracking per sprint.' },
  { Icon: BarChart3, title: 'Burndown & Velocity', desc: 'See remaining work against the ideal line, and compare committed vs. completed points sprint over sprint.' },
  { Icon: ShieldCheck, title: 'Role-Aware Access', desc: 'Keycloak-backed authentication with Admin, PM, Developer, Tester and DevOps roles enforced end to end.' },
  { Icon: GitBranch, title: 'Project & Team Management', desc: 'Create projects, assign teams and managers, and track status from Active to Completed.' },
  { Icon: Rocket, title: 'Milestone-Ready Architecture', desc: 'A service-layer boundary means new modules plug in without touching what already works.' }
]

export const STACK = ['React', 'Vite', 'Spring Boot', 'PostgreSQL', 'Keycloak', 'Docker', 'Hibernate/JPA']


export const STATS = [
  { value: '5', label: 'Role types enforced' },
  { value: '2', label: 'Milestones delivered' },
  { value: '100%', label: 'Milestone 1 preserved' },
  { value: '0', label: 'Breaking changes shipped' }
]

export const TESTIMONIALS = [
  { name: 'Namita',role:'Frontend Lead',quote:'Crafting responsive interfaces and intuitive component architecture made delivering an effortless user experience seamless.'},
  {name:'Sanika',role:'Frontend Lead',quote:'Architecting scalable frontend components and responsive layouts allowed our team to iterate rapidly without sacrificing UI performance.'},
  { name: 'Neha K.', role: 'Backend Architect', quote: 'The service-layer boundary meant the frontend team could build ahead without ever touching our APIs.' },
  { name: 'Jashanpreet S.', role: 'Server & Security Lead', quote: 'The real-time webhook syncs with GitHub Actions and Render make tracking our deployment pipelines and build logs completely effortless.' },
  {name:'Shalini.',role:'Database lead',quote:'Optimizing queries and tuning schemas so the application scales eeffortlessly without missing a beat.'}
]

export const FAQS = [
  { 
    q: 'What is NeuroForge?', 
    a: 'NeuroForge is a comprehensive project management and DevOps observability platform that brings together Kanban boards, CI/CD pipeline tracking, and system monitoring into a single unified workspace.' 
  },
  { 
    q: 'How does NeuroForge handle CI/CD pipelines?', 
    a: 'NeuroForge integrates seamlessly with GitHub Actions to provide live build statuses, deployment frequency analytics, and detailed pipeline logs directly within your project dashboard.' 
  },
  { 
    q: 'Can I monitor my infrastructure and applications?', 
    a: 'Yes, NeuroForge ties into Prometheus and Grafana to deliver real-time observability, live environment health metrics, and customizable alert rules.' 
  },
  { 
    q: 'What kind of project management tools are included?', 
    a: 'The platform offers robust agile tools including Kanban boards, sprint and milestone tracking, blocker flagging, and AI-assisted analytics to help optimize your team\'s workflow.' 
  },
  { 
    q: 'Is role-based access control (RBAC) supported?', 
    a: 'Absolutely. NeuroForge uses Keycloak-backed authentication to ensure secure logins, protected routes, and strict role-based access control across all workspaces and teams.' 
  }
];