import { hashForPage } from "../../../domain/permissions"
export function Forbidden({ page = "this screen", home = "Counter" }: { page?: string; home?: string }) { return <main className="workspace-screen forbidden-screen" role="alert"><h1>403 · FORBIDDEN</h1><p>Your role cannot open {page}.</p><a href={hashForPage(home)}>Return to your home screen</a></main> }
