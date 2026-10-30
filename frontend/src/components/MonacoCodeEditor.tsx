import { useEffect, useRef } from 'react'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/editor/editor.worker?worker'
import tsWorker from 'monaco-editor/language/typescript/ts.worker?worker'

type WorkerGlobal = typeof globalThis & {
  MonacoEnvironment?: { getWorker: (workerId: string, label: string) => Worker }
}

;(self as WorkerGlobal).MonacoEnvironment = {
  getWorker: (_workerId, label) =>
    label === 'typescript' || label === 'javascript' ? new tsWorker() : new editorWorker(),
}

export default function MonacoCodeEditor({
  value,
  language,
  fontSize,
  theme,
  onChange,
  onRun,
  onSubmit,
}: {
  value: string
  language: string
  fontSize: number
  theme: 'light' | 'dark'
  onChange: (value: string) => void
  onRun: () => void
  onSubmit: () => void
}) {
  const host = useRef<HTMLDivElement>(null)
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null)
  const initial = useRef({ value, language, fontSize, theme })
  const onChangeRef = useRef(onChange)
  const onRunRef = useRef(onRun)
  const onSubmitRef = useRef(onSubmit)

  useEffect(() => {
    onChangeRef.current = onChange
    onRunRef.current = onRun
    onSubmitRef.current = onSubmit
  }, [onChange, onRun, onSubmit])

  useEffect(() => {
    if (!host.current) return
    const editor = monaco.editor.create(host.current, {
      value: initial.current.value,
      language: initial.current.language,
      theme: initial.current.theme === 'dark' ? 'vs-dark' : 'vs',
      fontSize: initial.current.fontSize,
      automaticLayout: true,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      tabSize: 4,
      insertSpaces: true,
      padding: { top: 12, bottom: 12 },
      renderLineHighlight: 'line',
      smoothScrolling: true,
      fixedOverflowWidgets: true,
      ariaLabel: 'Code editor',
      fontFamily: "ui-monospace, 'Cascadia Code', Consolas, monospace",
      fontLigatures: false,
    })
    editorRef.current = editor
    const subscription = editor.onDidChangeModelContent(() => onChangeRef.current(editor.getValue()))
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => onRunRef.current())
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => onSubmitRef.current())
    return () => {
      subscription.dispose()
      editor.dispose()
      editorRef.current = null
    }
  }, [])

  useEffect(() => {
    const editor = editorRef.current
    if (editor && editor.getValue() !== value) editor.setValue(value)
  }, [value])

  useEffect(() => {
    const model = editorRef.current?.getModel()
    if (model) monaco.editor.setModelLanguage(model, language)
  }, [language])

  useEffect(() => {
    editorRef.current?.updateOptions({ fontSize })
  }, [fontSize])

  useEffect(() => {
    monaco.editor.setTheme(theme === 'dark' ? 'vs-dark' : 'vs')
  }, [theme])

  return <div className="monaco-host" ref={host} />
}
