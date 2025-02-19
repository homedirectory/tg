/**
@license
Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
This code may only be used under the BSD style license found at
http://polymer.github.io/LICENSE.txt The complete set of authors may be found at
http://polymer.github.io/AUTHORS.txt The complete set of contributors may be
found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by Google as
part of the polymer project is also subject to an additional IP rights grant
found at http://polymer.github.io/PATENTS.txt
*/
import "../iron-icon/iron-icon.js";
import "../iron-flex-layout/iron-flex-layout.js";
import "../paper-styles/default-theme.js";
import "../paper-styles/typography.js";
import { IronResizableBehavior } from "../iron-resizable-behavior/iron-resizable-behavior.js";
import { Polymer } from "../polymer/lib/legacy/polymer-fn.js";
import { dom } from "../polymer/lib/legacy/polymer.dom.js";
import { html } from "../polymer/lib/utils/html-tag.js";
/**
`<paper-badge>` is a circular text badge that is displayed on the top right
corner of an element, representing a status or a notification. It will badge
the anchor element specified in the `for` attribute, or, if that doesn't exist,
centered to the parent node containing it.

Badges can also contain an icon by adding the `icon` attribute and setting
it to the id of the desired icon. Please note that you should still set the
`label` attribute in order to keep the element accessible. Also note that you
will need to import the `iron-iconset` that includes the icons you want to use.
See [iron-icon](../iron-icon) for more information on how to import and use icon
sets.

Example:

    <div style="display:inline-block">
      <span>Inbox</span>
      <paper-badge label="3"></paper-badge>
    </div>

    <div>
      <paper-button id="btn">Status</paper-button>
      <paper-badge
          icon="favorite"
          for="btn"
          label="favorite icon">
      </paper-badge>
    </div>

    <div>
      <paper-icon-button
          id="account-box"
          icon="account-box"
          alt="account-box">
      </paper-icon-button>
      <paper-badge
          icon="social:mood"
          for="account-box"
          label="mood
          icon">
      </paper-badge>

    </div>

### Styling

The following custom properties and mixins are available for styling:

Custom property | Description | Default
----------------|-------------|----------
`--paper-badge-background` | The background color of the badge | `--accent-color`
`--paper-badge-opacity` | The opacity of the badge | `1.0`
`--paper-badge-text-color` | The color of the badge text | `white`
`--paper-badge-width` | The width of the badge circle | `20px`
`--paper-badge-height` | The height of the badge circle | `20px`
`--paper-badge-margin-left` | Optional spacing added to the left of the badge. | `0px`
`--paper-badge-margin-bottom` | Optional spacing added to the bottom of the badge. | `0px`
`--paper-badge` | Mixin applied to the badge | `{}`

@group Paper Elements
@element paper-badge
@demo demo/index.html
*/

Polymer({
  _template: html`
    <style>
      :host {
        display: block;
        position: absolute;
        outline: none;
      }

      :host([hidden]), [hidden] {
        display: none !important;
      }

      iron-icon {
        --iron-icon-width: 12px;
        --iron-icon-height: 12px;
      }

      .badge {
        @apply --layout;
        @apply --layout-center-center;
        @apply --paper-font-common-base;

        font-weight: normal;
        font-size: 11px;
        border-radius: 50%;
        margin-left: var(--paper-badge-margin-left, 0px);
        margin-bottom: var(--paper-badge-margin-bottom, 0px);
        width: var(--paper-badge-width, 20px);
        height: var(--paper-badge-height, 20px);
        background-color: var(--paper-badge-background, var(--accent-color));
        opacity: var(--paper-badge-opacity, 1.0);
        color: var(--paper-badge-text-color, white);

        @apply --paper-badge;
      }
    </style>

    <div class="badge">
      <iron-icon hidden\$="{{!_computeIsIconBadge(icon)}}" icon="{{icon}}"></iron-icon>
      <span id="badge-text" hidden\$="{{_computeIsIconBadge(icon)}}">{{label}}</span>
    </div>
  `,
  is: 'paper-badge',

  /** @private */
  hostAttributes: {
    role: 'status',
    tabindex: 0
  },
  behaviors: [IronResizableBehavior],
  listeners: {
    'iron-resize': 'updatePosition'
  },
  properties: {
    /**
     * The id of the element that the badge is anchored to. This element
     * must be a sibling of the badge.
     */
    for: {
      type: String,
      observer: '_forChanged'
    },

    /**
     * The label displayed in the badge. The label is centered, and ideally
     * should have very few characters.
     */
    label: {
      type: String,
      observer: '_labelChanged'
    },

    /**
     * An iron-icon ID. When given, the badge content will use an
     * `<iron-icon>` element displaying the given icon ID rather than the
     * label text. However, the label text will still be used for
     * accessibility purposes.
     */
    icon: {
      type: String,
      value: ''
    },
    _boundNotifyResize: {
      type: Function,
      value: function () {
        return this.notifyResize.bind(this);
      }
    },
    _boundUpdateTarget: {
      type: Function,
      value: function () {
        return this._updateTarget.bind(this);
      }
    }
  },
  attached: function () {
    // Polymer 2.x does not have this.offsetParent defined by attached
    requestAnimationFrame(this._boundUpdateTarget);
  },
  attributeChanged: function (name) {
    if (name === 'hidden') {
      this.updatePosition();
    }
  },
  _forChanged: function () {
    // The first time the property is set is before the badge is attached,
    // which means we're not ready to position it yet.
    if (!this.isAttached) {
      return;
    }

    this._updateTarget();
  },
  _labelChanged: function () {
    this.setAttribute('aria-label', this.label);
  },
  _updateTarget: function () {
    this._target = this.target;
    requestAnimationFrame(this._boundNotifyResize);
  },
  _computeIsIconBadge: function (icon) {
    return icon.length > 0;
  },

  /**
   * Returns the target element that this badge is anchored to. It is
   * either the element given by the `for` attribute, or the immediate parent
   * of the badge.
   */
  get target() {
    var parentNode = dom(this).parentNode; // If the parentNode is a document fragment, then we need to use the host.

    var ownerRoot = dom(this).getOwnerRoot();
    var target;

    if (this.for) {
      target = dom(ownerRoot).querySelector('#' + this.for);
    } else {
      target = parentNode.nodeType == Node.DOCUMENT_FRAGMENT_NODE ? ownerRoot.host : parentNode;
    }

    return target;
  },

  /**
   * Repositions the badge relative to its anchor element. This is called
   * automatically when the badge is attached or an `iron-resize` event is
   * fired (for exmaple if the window has resized, or your target is a
   * custom element that implements IronResizableBehavior).
   *
   * You should call this in all other cases when the achor's position
   * might have changed (for example, if it's visibility has changed, or
   * you've manually done a page re-layout).
   */
  updatePosition: function () {
    if (!this._target || !this.offsetParent) {
      return;
    }

    var parentRect = this.offsetParent.getBoundingClientRect();

    var targetRect = this._target.getBoundingClientRect();

    var thisRect = this.getBoundingClientRect();
    this.style.left = targetRect.left - parentRect.left + (targetRect.width - thisRect.width / 2) + 'px';
    this.style.top = targetRect.top - parentRect.top - thisRect.height / 2 + 'px';
  }
});