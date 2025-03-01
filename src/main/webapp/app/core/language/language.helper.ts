import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { LANGUAGES } from './language.constants';
import { BehaviorSubject, Observable } from 'rxjs';
import { captureException } from '@sentry/browser';

@Injectable({ providedIn: 'root' })
export class JhiLanguageHelper {
    private renderer: Renderer2;
    private _language: BehaviorSubject<string>;

    constructor(private translateService: TranslateService, private titleService: Title, private router: Router, rootRenderer: RendererFactory2) {
        this._language = new BehaviorSubject<string>(this.translateService.currentLang);
        this.renderer = rootRenderer.createRenderer(document.querySelector('html'), null);
        this.init();
    }

    /**
     * Get all supported ISO_639-1 language codes.
     */
    getAll(): string[] {
        return LANGUAGES;
    }

    get language(): Observable<string> {
        return this._language.asObservable();
    }

    /**
     * Update the window title using a value from the following order:
     * 1. The function's titleKey parameter
     * 2. The return value of {@link getPageTitle}, extracting it from the router state or a fallback value
     * If the translation doesn't exist, a Sentry exception is thrown.
     */
    updateTitle(titleKey?: string) {
        if (!titleKey) {
            titleKey = this.getPageTitle(this.router.routerState.snapshot.root);
        }

        this.translateService.get(titleKey).subscribe((title) => {
            if (title) {
                this.titleService.setTitle(title);
            } else {
                captureException(new Error(`Translation key '${titleKey}' for page title not found`));
            }
        });
    }

    private init() {
        this.translateService.onLangChange.subscribe(() => {
            this._language.next(this.translateService.currentLang);
            this.renderer.setAttribute(document.querySelector('html'), 'lang', this.translateService.currentLang);
            this.updateTitle();
        });
    }

    /**
     * Get the current page's title key based on the router state.
     * Fallback to 'global.title' when no key is found.
     * @param routeSnapshot The snapshot of the current route
     */
    getPageTitle(routeSnapshot: ActivatedRouteSnapshot) {
        let title: string = routeSnapshot.data?.['pageTitle'] || 'global.title';
        if (routeSnapshot.firstChild) {
            title = this.getPageTitle(routeSnapshot.firstChild) || title;
        }
        return title;
    }
}
